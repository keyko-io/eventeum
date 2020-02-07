package io.keyko.monitoring.agent.core.dto.event.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodParameterDefinition implements Comparable<MethodParameterDefinition>, Serializable {

    Integer position;

    @Embedded
    ParameterType type;

    String name;

    private String value;

    @Override
    public int compareTo(MethodParameterDefinition o) {
        return this.position.compareTo(o.getPosition());
    }
}
