package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.map.MappingFramework.CellValueProducer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A selection of {@link CellValueProducer} to {@link JavaToDataCellConverter} to write a certain value from
 * {@link Source} to a {@link DataCell}.
 *
 * @author Jonathan Hale
 */
public class ProductionPath {
    public final JavaToDataCellConverterFactory<?> m_factory;

    public final CellValueProducer<?, ?, ?> m_producer;

    /**
     * Constructor.
     *
     * @param f Factory of the converter used to extract a Java value out a DataCell.
     * @param c CellValueConsumer which accepts the Java value extracted by the converter and writes it to some
     *            {@link Destination}.
     */
    public ProductionPath(final JavaToDataCellConverterFactory<?> f, final CellValueProducer<?, ?, ?> c) {
        this.m_factory = f;
        this.m_producer = c;
    }

    @Override
    public String toString() {
        return String.format("%s ---> %s --(\"%s\")-> %s", m_producer.getIdentifier(), m_factory.getSourceType().getSimpleName(),
            m_factory.getName(), m_factory.getDestinationType().getName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_producer == null) ? 0 : m_producer.hashCode());
        result = prime * result + ((m_factory == null) ? 0 : m_factory.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ProductionPath other = (ProductionPath)obj;
        if (m_producer == null) {
            if (other.m_producer != null) {
                return false;
            }
        } else if (!m_producer.equals(other.m_producer)) {
            return false;
        }
        if (m_factory == null) {
            if (other.m_factory != null) {
                return false;
            }
        } else if (!m_factory.equals(other.m_factory)) {
            return false;
        }
        return true;
    }

    /**
     * Serialize to node settings.
     *
     * @param settings Settings to save to
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("conversion_factory", m_factory.getIdentifier());
        settings.addString("external_type", m_producer.getIdentifier());
    }

    /**
     * Deserialize from node settings.
     *
     * @param settings Settings to load from
     * @throws InvalidSettingsException If invalid settings are encountered.
     */
    void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String factoryId = settings.getString("conversion_factory", null);
        final String externalType = settings.getString("external_type", null);

        // TODO
    }
}
