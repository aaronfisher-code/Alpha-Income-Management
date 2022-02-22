package components.layouts;

import javafx.scene.Node;

public class BootstrapColumn {

    private final Node content;

    int[] columnWidths = new int[]{
            1,  //XS (default)
            -1, //Sm
            -1, //Md
            -1, //Lg
            -1  //XL
    };

    public BootstrapColumn(Node content) {
        this.content = content;
    }

    /**
     * Set the column width of the content at the specified breakpoint
     *
     * @param breakPoint the screen size break point being specified
     * @param width      the requested width at this breakpoint (must be between 1 and 12);
     */
    public void setBreakpointColumnWidth(Breakpoint breakPoint, int width) {
        columnWidths[breakPoint.getValue()] = clamp(width, 1, 12);
    }

    /**
     * Remove a previously-specified column breakpoint width setting
     *
     * @param breakPoint the breakpoint to reset
     */
    public void unsetBreakPoint(Breakpoint breakPoint) {
        columnWidths[breakPoint.getValue()] = -1;
    }

    /**
     * Reset all column width break points, so the default width at all break points is 1.
     */
    public void unsetAllBreakPoints() {
        this.columnWidths = new int[]{
                1,  //XS (default)
                -1, //Sm
                -1, //Md
                -1, //Lg
                -1  //XL
        };
    }

    /**
     * Iterate through breakpoints, beginning at the specified bp, travelling down. Return first valid bp value.
     * If none are valid, return 1
     *
     * @param breakPoint the breakpoint at which to determine the column width
     * @return the requested width at that breakpoint, or based on a lower breakpoint if the specified bp has not been set.
     */
    public int getColumnWidth(Breakpoint breakPoint) {

        //Iterate through breakpoints, beginning at the specified bp, travelling down. Return first valid bp value.
        for (int i = breakPoint.getValue(); i >= 0; i--) {
            if (isValid(columnWidths[i])) return columnWidths[i];
        }

        //If none are valid, return 1
        return 1;
    }

    /**
     * Get the node in this column
     *
     * @return the content.
     */
    public Node getContent() {
        return content;
    }

    /**
     * Whether a value is between 1 and 12 (i.e. a valid column width)
     *
     * @param value the value being tested
     * @return whether the value is a valid column width
     */
    private boolean isValid(int value) {
        return value > 0 && value <= 12;
    }

    /**
     * Return an integer between the stated max and min values
     *
     * @param value the value to be clamped
     * @param max   the maximum allowed integer value to be returned
     * @param min   the minimum allowed integer value to be returned
     * @return the clamped value
     */
    public static int clamp(int value, int min, int max) {

        if (max < min) throw new IllegalArgumentException("Cannot clamp when max is greater than min");

        if (value > max) {
            return max;
        } else if (value < min) {
            return min;
        } else {
            return value;
        }
    }
}
