package explorableviz.transparenttext.paragraph;

import explorableviz.transparenttext.variable.Variables;
import static explorableviz.transparenttext.Program.replaceVariables;

public class Literal extends TextFragment {

    private final SelectedRegion selectedRegion;
    public Literal(String value, SelectedRegion selectedRegion) {
        super(value);
        this.selectedRegion = selectedRegion;
    }
    @Override
    public TextFragment replace(Variables computedVariables) {
        return new Literal(replaceVariables(getValue(), computedVariables), null);
    }

    public SelectedRegion getSelectedRegion() {
        return selectedRegion;
    }

    public record SelectedRegion(int start, int end) {
    }

}
