package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.map.MappingFramework.CellValueConsumer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Interface for the destination for extracting and writing {@link DataCell}/{@link DataValue} contents.
 *
 * May be implemented to write the values to an SQL Database, file, H2O frame and more.
 *
 * @author Jonathan Hale
 */
public interface Destination {

    /**
     * Parameters passed to a {@link CellValueConsumer}, further specializing it to a specific input and output.
     *
     * @author Jonathan Hale
     */
    public static interface ConsumerParameters<DestinationType extends Destination> {

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
