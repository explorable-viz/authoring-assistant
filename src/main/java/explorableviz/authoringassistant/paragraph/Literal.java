package explorableviz.authoringassistant.paragraph;

public class Literal extends TextFragment {

    private final SelectedRegion selectedRegion;
    public Literal(String value, SelectedRegion selectedRegion) {
        super(value);
        this.selectedRegion = selectedRegion;
    }

    public SelectedRegion getSelectedRegion() {
        return selectedRegion;
    }

    public record SelectedRegion(int start, int end) {
    }

}
