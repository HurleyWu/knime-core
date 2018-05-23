package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.convert.map.MappingFramework.CellValueProducer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Interface for data sources from which to create KNIME {@link DataCell data cells}.
 *
 * May be implemented to read values from an SQL Database, file, H2O frame and more.
 *
 * Note: extend {@link AbstractSource} instead of implementing this interface.
 *
 * @author Jonathan Hale
 * @param <ThisType> Class which is implementing this interface.
 */
public interface Source {
    /**
     * Parameters passed to a {@link CellValueProducer}, further specializing it to a specific input column.
     *
     * @author Jonathan Hale
     * @param <S> Type of {@link Source} these parameters work with
     */
    public static interface ProducerParameters<S extends Source> {

        /**
         * Save these parameters to node settings.
         *
         * @param settings Settings to save to
         */
        void saveSettingsTo(NodeSettingsWO settings);

        /**
         * Load parameters from node settings.
         *
         * @param settings Settings to load from
         * @throws InvalidSettingsException If invalid settings are encountered.
         */
        void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException;
    }
}
