package exp.nefor.client.command;

import exp.nefor.client.config.ConfigManager;
import exp.nefor.client.util.player.chat.ChatUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public final class CommandManager {
    private static final String PREFIX = ".";
    private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();

    public interface Command { void run(String[] args); String help(); }

    static {
        register("cfg", new CfgCommand());
        register("friend", new FriendCommand());
        register("macro", new MacroCommand());
        register("bot", new Command(){
            public void run(String[] a){
                var mod = exp.nefor.client.module.ModuleManager.get(exp.nefor.client.module.impl.player.Bot.class);
                if(mod == null){ ChatUtil.sendMessageToClient("§cBot module missing", false); return; }
                if(a.length == 0){
                    ChatUtil.sendMessageToClient("§7.botsay <nick|*> <text> §8- сказать от лица бота", false);
                    return;
                }
                String sub = a[0].toLowerCase();
                if((sub.equals("say") || sub.equals("botsay")) && a.length >= 3){
                    String who = a[1];
                    String text = String.join(" ", java.util.Arrays.copyOfRange(a, 2, a.length));
                    int n = 0;
                    for(var en : mod.getBots()){
                        if(who.equals("*") || en.nick().equalsIgnoreCase(who)){
                            en.conn.sendChat(text);
                            n++;
                        }
                    }
                    ChatUtil.sendMessageToClient(n > 0 ? "§aSent as bot" : "§cNo such bot", false);
                    return;
                }
                
                String who = a[0];
                String text = String.join(" ", java.util.Arrays.copyOfRange(a, 1, a.length));
                int n = 0;
                for(var en : mod.getBots()){
                    if(who.equals("*") || en.nick().equalsIgnoreCase(who)){
                        en.conn.sendChat(text);
                        n++;
                    }
                }
                ChatUtil.sendMessageToClient(n > 0 ? "§aSent as bot" : "§cUsage: .bot <nick|*> <text>", false);
            }
            public String help(){ return ".bot <nick|*> <text> - сказать от лица бота"; }
        });
        register("help", new Command(){ public void run(String[] a){ ChatUtil.sendMessageToClient("§7--- Nefor Commands ---", false); COMMANDS.forEach((k,c)-> ChatUtil.sendMessageToClient("§b."+k+" §7- "+c.help(), false)); } public String help(){ return "list commands"; }});
        register("vclip", new Command(){ public void run(String[] a){ try{ var mc = net.minecraft.client.MinecraftClient.getInstance(); if(mc.player==null) return; double v = a.length>0? Double.parseDouble(a[0]): 5; mc.player.setPosition(mc.player.getX(), mc.player.getY()+v, mc.player.getZ()); ChatUtil.sendMessageToClient("§aVClipped "+v, false);}catch(Exception e){ ChatUtil.sendMessageToClient("§cUsage: .vclip <blocks>", false);} } public String help(){ return "vclip <y>"; } });
    }

    public static void register(String name, Command cmd){ COMMANDS.put(name.toLowerCase(), cmd); }
    public static void register(String name, Command cmd, String help){ COMMANDS.put(name.toLowerCase(), new Command(){ public void run(String[] a){cmd.run(a);} public String help(){return help;}}); }

    public static boolean handle(String message){
        if(!message.startsWith(PREFIX)) return false;
        String raw = message.substring(1).trim();
        if(raw.isEmpty()) return true;
        String[] split = raw.split("\\s+");
        String cmd = split[0].toLowerCase();
        String[] args = Arrays.copyOfRange(split, 1, split.length);
        Command c = COMMANDS.get(cmd);
        if(c==null){ ChatUtil.sendMessageToClient("§cUnknown command ."+cmd+"  try .help", false); return true; }
        try{ c.run(args);}catch(Exception e){ ChatUtil.sendMessageToClient("§cError: "+e.getMessage(), false);}
        return true;
    }

    
    static class CfgCommand implements Command{
        public String help(){ return ".cfg dir | add <name> | save [name] | load <name> | delete <name> | list"; }
        public void run(String[] a){
            if(a.length==0){ helpCfg(); return; }
            String sub = a[0].toLowerCase();
            switch(sub){
                case "dir","list" -> {
                    var list = ConfigManager.listConfigs();
                    ChatUtil.sendMessageToClient("§7Configs ("+list.size()+"): §a"+String.join(", ", list.isEmpty()? List.of("none"): list), false);
                    ChatUtil.sendMessageToClient("§7Current: §b"+ConfigManager.getCurrent()+" §7path: §8"+ConfigManager.configsDir().toString(), false);
                }
                case "save" -> {
                    String name = a.length>1? a[1]: ConfigManager.getCurrent();
                    ConfigManager.save(name);
                    ChatUtil.sendMessageToClient("§aSaved config §b"+name, false);
                }
                case "add","create" -> {
                    if(a.length<2){ ChatUtil.sendMessageToClient("§cUsage: .cfg add <name>", false); return; }
                    ConfigManager.save(a[1]);
                    ChatUtil.sendMessageToClient("§aCreated config §b"+a[1], false);
                }
                case "load","open" -> {
                    if(a.length<2){ ChatUtil.sendMessageToClient("§cUsage: .cfg load <name>", false); return; }
                    ConfigManager.load(a[1]);
                    ChatUtil.sendMessageToClient("§aLoaded config §b"+a[1], false);
                }
                case "delete","del","remove" -> {
                    if(a.length<2){ ChatUtil.sendMessageToClient("§cUsage: .cfg delete <name>", false); return; }
                    boolean ok = ConfigManager.delete(a[1]);
                    ChatUtil.sendMessageToClient(ok? "§aDeleted "+a[1]: "§cNot found "+a[1], false);
                }
                default -> helpCfg();
            }
        }
        void helpCfg(){ ChatUtil.sendMessageToClient("§7.cfg dir §8- list configs", false); ChatUtil.sendMessageToClient("§7.cfg add <name> §8- create", false); ChatUtil.sendMessageToClient("§7.cfg save [name] §8- save", false); ChatUtil.sendMessageToClient("§7.cfg load <name> §8- load", false); }
    }
    
    static class FriendCommand implements Command{
        public String help(){ return ".friend add <nick> | remove <nick> | list | clear"; }
        public void run(String[] a){
            if(a.length==0){ list(); return; }
            switch(a[0].toLowerCase()){
                case "add" -> { if(a.length<2){msg("§c/friend add <nick>"); return;} exp.nefor.client.system.FriendManager.add(a[1]); msg("§aAdded friend §b"+a[1]); }
                case "remove","del" -> { if(a.length<2){msg("§c/friend remove <nick>"); return;} exp.nefor.client.system.FriendManager.remove(a[1]); msg("§aRemoved §b"+a[1]); }
                case "list" -> list();
                case "clear" -> { exp.nefor.client.system.FriendManager.clear(); msg("§aCleared"); }
                default -> { 
                    String n=a[0]; if(exp.nefor.client.system.FriendManager.isFriend(n)){ exp.nefor.client.system.FriendManager.remove(n); msg("§cRemoved "+n);} else {exp.nefor.client.system.FriendManager.add(n); msg("§aAdded "+n);}
                }
            }
        }
        void list(){ var l=exp.nefor.client.system.FriendManager.getAll(); msg("§7Friends ("+l.size()+"): §a"+(l.isEmpty()?"none":String.join(", ",l))); }
        void msg(String s){ ChatUtil.sendMessageToClient(s,false); }
    }
    
    static class MacroCommand implements Command{
        public String help(){ return ".macro add <key> <text> | remove <key> | list"; }
        public void run(String[] a){
            if(a.length==0){ list(); return; }
            switch(a[0].toLowerCase()){
                case "add" -> {
                    if(a.length<3){msg("§c.macro add <key> <text>  e.g. .macro add g /hub"); return;}
                    String key=a[1].toUpperCase(); String text=String.join(" ", Arrays.copyOfRange(a,2,a.length));
                    exp.nefor.client.system.MacroManager.add(key,text); msg("§aMacro §b"+key+" §7-> §f"+text);
                }
                case "remove","del" -> { if(a.length<2){msg("§c.macro remove <key>");return;} exp.nefor.client.system.MacroManager.remove(a[1].toUpperCase()); msg("§aRemoved "+a[1]);}
                case "list" -> list();
                case "clear" -> { exp.nefor.client.system.MacroManager.clear(); msg("§aCleared macros"); }
                default -> msg("§7"+help());
            }
        }
        void list(){ var m=exp.nefor.client.system.MacroManager.getAll(); if(m.isEmpty()){msg("§7No macros"); return;} m.forEach((k,v)->msg("§b"+k+" §7-> §f"+v));}
        void msg(String s){ ChatUtil.sendMessageToClient(s,false); }
    }
}
