package cr.ac.tec.vizClone;

import lombok.Data;

@Data
public class CloneConfig {
    private boolean anyIdentifier = false;
    private boolean anyLiteral = true;

    public CloneConfig(boolean anyIdentifier, boolean anyLiteral) {
        this.anyIdentifier = anyIdentifier;
        this.anyLiteral = anyLiteral;
    }
}
