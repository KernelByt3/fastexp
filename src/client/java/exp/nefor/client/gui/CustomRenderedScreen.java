package exp.nefor.client.gui;

/**
 * Screens implementing this are drawn from the end-of-frame hook,
 * where immediate OpenGL output cannot be overwritten by the
 * vanilla GUI pipeline.
 */
public interface CustomRenderedScreen {

    void nefor$renderOverlay();
}
