package io.keyko.monitoring.agent.core.dto.event.parameter;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

/**
 * A textual based EventParameter, represented by a String.
 *
 * @author Craig Williams <craig.williams@consensys.net>
 */
@Embeddable
@Data
@NoArgsConstructor
public class StringParameter extends AbstractEventParameter<String> {

    public StringParameter(String type, String value, String name) {
        super(type, value, name);
    }

    public void setName(String name){
        this.name= name;
    }

    @Override
    public String getValueString() {
        return getValue();
    }
}
