package io.keyko.monitoring.agent.core.dto.event.parameter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.keyko.monitoring.agent.core.dto.parameter.AbstractParameter;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import java.util.ArrayList;

/**
 * An array EventParameter, backed by an ArrayList.
 * Its ArrayList rather than List as ArrayList implements Serializable.
 *
 * @author Craig Williams <craig.williams@consensys.net>
 */
@Embeddable
@Data
@NoArgsConstructor
public class ArrayParameter<T extends EventParameter<?>> extends AbstractParameter<ArrayList<T>> {

    @JsonIgnore
    private String stringRepresentation;

    public ArrayParameter(String entryType, Class<T> arrayParameterType, ArrayList<T> value, String name) {
        super(entryType + "[]", value, name);

        initStringRepresentation();
    }

    @Override
    public String getValueString() {
        return stringRepresentation;
    }

    private void initStringRepresentation() {
        final StringBuilder builder = new StringBuilder("[");

        getValue().forEach(param -> {
            builder.append("\"");
            builder.append(param.getValueString());
            builder.append("\"");
            builder.append(",");
        });

        builder.deleteCharAt(builder.length() - 1);
        builder.append("]");

        stringRepresentation = builder.toString();
    }
}
