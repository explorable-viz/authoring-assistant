package explorableviz.transparenttext.paragraph;

public class SelectedRegion {
    private final int start;
    private final int end;

    public SelectedRegion(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
