package explorableviz.transparenttext.paragraph;

import explorableviz.transparenttext.variable.Variables;

import java.util.Optional;

import static explorableviz.transparenttext.Program.replaceVariables;

public class Literal extends TextFragment {

    private final Optional<SelectedRegion> selectedRegion;
    public Literal(String value, Optional<SelectedRegion> selectedRegion) {
        super(value);
        this.selectedRegion = selectedRegion;
    }
    @Override
    public TextFragment replace(Variables computedVariables) {
        return new Literal(replaceVariables(getValue(), computedVariables), Optional.empty());
    }

    public Optional<SelectedRegion> getSelectedRegion() {
        return selectedRegion;
    }
}
