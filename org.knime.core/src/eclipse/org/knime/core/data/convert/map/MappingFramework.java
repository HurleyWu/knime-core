package org.knime.core.data.convert.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.map.Destination.ConsumerParameters;
import org.knime.core.data.convert.map.Source.ProducerParameters;
import org.knime.core.node.NodeLogger;

/**
 * Framework to map KNIME to external types vice versa.
 *
 * A frequent use case for KNIME nodes is to write or read data from an external storage. (A storage can be used as a
 * {@link Source source} or {@link Destination destination}.) In this case the value held by a KNIME {@link DataCell}
 * needs to be mapped to an external value.
 *
 * Extracting the Java value from a data cell is solved with {@link DataCellToJavaConverterRegistry} and
 * {@link JavaToDataCellConverterRegistry}. As custom KNIME extensions may implement custom {@link DataType data types},
 * it is impossible for nodes to support all possible types. Asking the custom type provider to implement mapping to all
 * different external sources and destinations (Python, SQL, H2O, Java, R and many more) is an impossible burdon and
 * therefore we introduce Java objects as an intermediary representation for the mappings. An external type provider can
 * implement a {@link DataCellToJavaConverterFactory} or {@link JavaToDataCellConverterFactory} to extract a Java object
 * out of his custom cell and implementors or nodes which write to SQL, H2O, R, Python, etc. can then provide mapping to
 * a more limited set of Java types. Some cells may not be able to create a known java type (e.g. only some
 * VeryChemicalType) -- in which case we cannot support the type -- but usually such a type can be serialized to a blob
 * or String.
 *
 * Some external types do not have a Java representation. We can therefore not simply map from Java object to a Java
 * representation of an external type. Instead we wrap the concepts of {@link Source source} and {@link Destination
 * destination} and the concepts of "how to write to" ({@link Consumer}) and "how to read from" ({@link Producer}) them.
 * The destination and source are external equivalent to a KNIME input or output table. How to write/read from them is
 * defined per set of types, but configurable via {@link ConsumerParameters}/{@link ProducerParameters}. These
 * parameters may include column index and row index, but are fully dependent on the type of external storage and meant
 * as a way in which the node communicates with an instance of {@link Consumer} or {@link Producer}. (TODO)
 *
 * With databases as storage, we have another special case where some databases are specializations of other databases
 * (e.g. Oracle databases a specialization of SQL databases). The specialiation may support additional Java types or
 * additional external types.
 *
 * Finally, all mapping must be allowed to be done explicitly. Since automatic mappings are often not useful, a user
 * needs control of how to map certain KNIME types to the external types (the intermediate java representation is not
 * relevant) -- via a node dialog panel for example.
 *
 * For the dialog panel to be as generic and reusable as possible, its only input should be the {@link DataTableSpec}
 * and the type of destination or source. It then presents the user a per column list of available external types to map
 * to. This list can be queried from the framework using {@link #getAvailableConsumptionPaths(Class<?>, DataType)} or
 * {@link #getAvailableProductionPaths(Class<?>, DataType)}. Both return a list of glorified pairs which also contain
 * the information on how the intermidate java representation is extracted from or wrapped into a {@link DataCell}.
 * These can then be serialized from the dialog and read in a node model.
 *
 * <h1>Usage</h1>
 *
 * Writing values to a {@link Destination}:
 *
 * <code lang="java"><pre>
 * // One time setup, e.g. in plugin initialisation:
 * MappingFramework.forDestinationType(OracleSQLDatabaseDest.class) //
 *      .setParent(SQLDatabaseDest.class) // inherit less specific consumers
 *      .registerConsumer(Integer.class, intConsumer) //
 *      .registerConsumer(String.class, stringConsumer);
 *
 * // Then many times:
 * TODO
 * </pre></code>
 *
 * Reading values from a {@link Source}:
 *
 * <code><pre>
 * // One time setup, e.g. in plugin initialisation:
 * MappingFramework.forSourceType(MySourceType.class) //
 *      .setParent(MyParentSourceType.class) // inherit less specific producers
 *      .registerProducer(Integer.class, intProducer) //
 *      .registerProducer(String.class, stringProducer);
 *
 * // Then many times:
 * TODO
 * </pre></code>
 *
 * @author Jonathan Hale
 */
public class MappingFramework {

    private static NodeLogger m_logger = NodeLogger.getLogger(MappingFramework.class);

    /* Do not instantiate */
    private MappingFramework() {
    }

    /**
     * Exception thrown when either a {@link CellValueProducer} or converter was missing for a type which needed to be
     * converted.
     *
     * @author Jonathan Hale
     */
    public static class UnmappableTypeException extends Exception {

        /* Generated serial version UID */
        private static final long serialVersionUID = 6498668986346262079L;

        private final DataType m_type;

        private final Class<?> m_javaType;

        /**
         * Constructor
         *
         * @param message Error message
         * @param type Type that could not be mapped.
         */
        public UnmappableTypeException(final String message, final DataType type) {
            this(message, type, null);
        }

        /**
         * Constructor
         *
         * @param message Error message
         * @param javaType Java type that could not be mapped
         */
        public UnmappableTypeException(final String message, final Class<?> javaType) {
            this(message, null, javaType);
        }

        /**
         * Constructor
         *
         * @param message Error message
         * @param type Type that could not be mapped.
         * @param javaType Java type that could not be mapped
         */
        public UnmappableTypeException(final String message, final DataType type, final Class<?> javaType) {
            super(message);

            m_type = type;
            m_javaType = javaType;
        }

        /**
         * @return The data type that was not mappable. May be <code>null</code>.
         */
        public DataType getDataType() {
            return m_type;
        }

        /**
         * @return The java type that was not mappable. May be <code>null</code>.
         */
        public Class<?> getJavaType() {
            return m_javaType;
        }
    }

    /**
     * A cell value consumer receives a Java value and writes it to a {@link Destination} using a certain external type.
     *
     * @author Jonathan Hale
     * @param <DestinationType> Type of {@link Destination} this consumer writes to
     * @param <T> Type of Java value the consumer accepts
     * @param <SP> Subtype of {@link ConsumerParameters} that can be used to configure this consumer
     */
    public static interface CellValueConsumer<DestinationType extends Destination, T, SP extends Destination.ConsumerParameters<DestinationType>> {

        /**
         * Writes the <code>value</code> to <code>destination</code> using given <code>destinationParams</code>.
         *
         * @param destination The {@link Destination}.
         * @param value The value to write.
         * @param destinationParams The parameters further specifying how to write to the destination, e.g. to which SQL
         *            column or table to write. Specific to the type of {@link Destination} and
         *            {@link CellValueConsumer} that is being used.
         */
        public void consumeCellValue(final DestinationType destination, final T value, final SP destinationParams);

        /**
         * Get identifier for the external type this consumer writes to
         *
         * @return the external type identifier
         */
        public String getIdentifier();
    }

    /**
     * Simple implementation of {@link CellValueConsumer} that allows passing the consumption procedure as a lambda.
     *
     * @author Jonathan Hale
     */
    public static class SimpleCellValueConsumer<DestinationType extends Destination, T, SP extends ConsumerParameters<DestinationType>>
        implements CellValueConsumer<DestinationType, T, SP> {

        final String m_externalTypeIdentifier;

        @FunctionalInterface
        public static interface CellValueConsumerLambda<DestinationType extends Destination, T, SP extends ConsumerParameters<DestinationType>> {

            /** @see CellValueConsumer#consumeCellValue() */
            @SuppressWarnings("javadoc")
            public void consumeCellValue(final DestinationType destination, final T value, final SP destinationParams);
        }

        final CellValueConsumerLambda<DestinationType, T, SP> m_lambda;

        /**
         * Constructor
         */
        public SimpleCellValueConsumer(final String externalType,
            final CellValueConsumerLambda<DestinationType, T, SP> lambda) {
            m_externalTypeIdentifier = externalType;
            m_lambda = lambda;
        }

        @Override
        public String getIdentifier() {
            return m_externalTypeIdentifier;
        }

        @Override
        public void consumeCellValue(final DestinationType destination, final T value, final SP destinationParams) {
            m_lambda.consumeCellValue(destination, value, destinationParams);
        }
    }

    /**
     * A cell value producer fetches a value from a {@link Source} which then can be written to a KNIME DataCell.
     *
     * @author Jonathan Hale
     * @param <SourceType> Type of {@link Source} this consumer writes to
     * @param <T> Type of Java value the consumer accepts
     * @param <SP> Subtype of {@link Source.ProducerParameters} that can be used to configure this consumer
     */
    public static interface CellValueProducer<SourceType extends Source, T, SP extends Source.ProducerParameters<SourceType>> {

        /**
         * Reads the <code>value</code> to <code>destination</code> using given <code>destinationParams</code>.
         *
         * @param source The {@link Source}.
         * @param params The parameters further specifying how to read from the {@link Source}, e.g. to which SQL column
         *            or table to read from. Specific to the type of {@link Source} and {@link CellValueProducer} that
         *            is being used.
         * @return The value which was read from source
         */
        public T produceCellValue(final SourceType source, final SP params);

        /**
         * Get identifier for the external type this consumer writes to
         *
         * @return the external type identifier
         */
        public String getIdentifier();
    }

    /**
     * Simple implementation of {@link CellValueProducer} that allows passing the production function as a lambda
     *
     * @author Jonathan Hale
     */
    public static class SimpleCellValueProducer<SourceType extends Source, T, SP extends ProducerParameters<SourceType>>
        implements CellValueProducer<SourceType, T, SP> {

        final String m_externalTypeIdentifier;

        @FunctionalInterface
        public static interface CellValueProducerLambda<SourceType extends Source, T, SP extends ProducerParameters<SourceType>> {

            /** @see CellValueProducer#produceCellValue() */
            @SuppressWarnings("javadoc")
            public T produceCellValue(final SourceType source, final SP sourceParams);
        }

        final CellValueProducerLambda<SourceType, T, SP> m_lambda;

        /**
         * Constructor
         */
        public SimpleCellValueProducer(final String externalType,
            final CellValueProducerLambda<SourceType, T, SP> lambda) {
            m_externalTypeIdentifier = externalType;
            m_lambda = lambda;
        }

        @Override
        public String getIdentifier() {
            return m_externalTypeIdentifier;
        }

        @Override
        public T produceCellValue(final SourceType source, final SP sourceParams) {
            return m_lambda.produceCellValue(source, sourceParams);
        }
    }

    /**
     * Per destination type consumer registry.
     *
     * Place to register consumers for a specific destination type.
     *
     * @author Jonathan Hale
     * @param <DestinationType> Type of {@link Destination} for which this registry holds consumers.
     */
    public static class ConsumerRegistry<DestinationType extends Destination> {
        private HashMap<Class<?>, HashMap<String, CellValueConsumer<DestinationType, ?, ?>>> m_consumers =
            new HashMap<>();

        private Class<DestinationType> m_destinationType;

        /* Parent registry. */
        private ConsumerRegistry<?> m_parent = null; // TODO A dummy/default/empty registry would be safer

        /**
         * Constructor
         *
         * @param destinationType Class of the destination type of this registry. Used for log messages.
         */
        protected ConsumerRegistry(final Class<DestinationType> destinationType) {
            m_destinationType = destinationType;
        }

        /**
         * Set parent destination type.
         *
         * Makes this registry inherit all consumers of the parent type. Will always priorize consumers of the more
         * specialized type.
         *
         * @param parentType type of {@link Destination}, which should be this types parent.
         */
        public ConsumerRegistry<DestinationType> setParent(final Class<? extends Destination> parentType) {
            m_parent = MappingFramework.forDestinationType(parentType);
            return this;
        }

        /**
         * Register a consumer to make it available to {@link Destination}s of
         *
         * @param sourceType Type this {@link CellValueConsumer} can accept.
         * @param consumer The {@link CellValueConsumer}.
         * @return self (for method chaining)
         */
        public ConsumerRegistry<DestinationType> registerConsumer(final Class<?> sourceType,
            final CellValueConsumer<DestinationType, ?, ?> consumer) {
            m_consumers.putIfAbsent(sourceType, new HashMap<>());
            if (m_consumers.get(sourceType).putIfAbsent(consumer.getIdentifier(), consumer) != null) {
                m_logger.warn(String.format(
                    "A %s CellValueConsumer was already registered for destination type %s and source type %, skipping.",
                    sourceType.getSimpleName(), m_destinationType.getSimpleName(), consumer.getIdentifier()));
            }

            return this;
        }

        /**
         * Get a certain {@link CellValueConsumer}.
         *
         * @param sourceType Source type that should be consumable by the returned {@link CellValueConsumer}.
         * @return a {@link CellValueConsumer} matching the given criteria, or <code>null</code> if none matches them.
         */
        public <T, SP extends ConsumerParameters<DestinationType>> CellValueConsumer<DestinationType, T, SP>
            get(final Class<T> sourceType, final String externalType) {
            final HashMap<String, CellValueConsumer<DestinationType, ?, ?>> map = m_consumers.get(sourceType);

            if (map == null) {
                return m_parent != null
                    ? (CellValueConsumer<DestinationType, T, SP>)m_parent.get(sourceType, externalType) : null;
            }

            final CellValueConsumer<DestinationType, T, SP> consumer =
                (CellValueConsumer<DestinationType, T, SP>)map.get(externalType);
            if (consumer == null && m_parent != null) {
                return (CellValueConsumer<DestinationType, T, SP>)m_parent.get(sourceType, externalType);
            }
            return consumer;
        }

        /**
         * @param type Data type that should be converted.
         * @return List of conversion paths
         */
        public Collection<ConsumptionPath> getAvailableConsumptionPaths(final DataType type) {
            final ArrayList<ConsumptionPath> cp = new ArrayList<>();

            for (final DataCellToJavaConverterFactory<?, ?> f : DataCellToJavaConverterRegistry.getInstance()
                .getFactoriesForSourceType(type)) {
                if (!m_consumers.containsKey(f.getDestinationType())) {
                    continue;
                }
                for (final CellValueConsumer<DestinationType, ?, ?> c : m_consumers.get(f.getDestinationType())
                    .values()) {
                    if (c != null) {
                        cp.add(new ConsumptionPath(f, c));
                    }
                }
            }

            return cp;
        }

        /**
         * Unregister all consumers
         *
         * @return self (for method chaining)
         */
        public ConsumerRegistry<DestinationType> unregisterAllConsumers() {
            m_consumers.clear();
            return this;
        }
    }

    /**
     * Per source type producer registry.
     *
     * Place to register consumers for a specific destination type.
     *
     * @author Jonathan Hale
     * @param <SourceType> Type of {@link Destination} for which this registry holds consumers.
     */
    public static class ProducerRegistry<SourceType extends Source> {
        private HashMap<String, Map<Class<?>, CellValueProducer<SourceType, ?, ?>>> m_producers = new HashMap<>();

        private Class<SourceType> m_sourceType;

        private ProducerRegistry<?> m_parent;

        /**
         * Constructor
         *
         * @param sourceType Class of the source type of this registry. Used for log messages.
         */
        protected ProducerRegistry(final Class<SourceType> sourceType) {
            m_sourceType = sourceType;
        }

        /**
         * Set parent source type.
         *
         * Makes this registry inherit all producers of the parent type. Will always priorize producers of the more
         * specialized type.
         *
         * @param parentType type of {@link Destination}, which should be this types parent.
         */
        public ProducerRegistry<SourceType> setParent(final Class<? extends Source> parentType) {
            m_parent = MappingFramework.forSourceType(parentType);
            return this;
        }

        /**
         * Register a consumer to make it available to {@link Destination}s of
         *
         * @param destType Type this {@link CellValueConsumer} can accept.
         * @param producer The {@link CellValueConsumer}.
         * @return self (for method chaining)
         */
        public ProducerRegistry<SourceType> registerProducer(final Class<?> destType,
            final CellValueProducer<SourceType, ?, ?> producer) {
            m_producers.putIfAbsent(producer.getIdentifier(), new HashMap<>());

            if (m_producers.get(producer.getIdentifier()).putIfAbsent(destType, producer) != null) {
                m_logger
                    .warn(String.format("A %s CellValueProducer was already registered for source type %s, skipping.",
                        destType.getSimpleName(), m_sourceType.getSimpleName()));
            }
            return this;
        }

        /**
         * Get a certain {@link CellValueConsumer}.
         *
         * @param destType Source type that should be consumable by the returned {@link CellValueConsumer}.
         * @return a {@link CellValueConsumer} matching the given criteria, or <code>null</code> if none matches them.
         */
        public <T, SP extends ProducerParameters<SourceType>> CellValueProducer<SourceType, T, SP>
            get(final String externalType, final Class<T> destType) {
            final Map<Class<?>, CellValueProducer<SourceType, ?, ?>> producer = m_producers.get(externalType);
            return (CellValueProducer<SourceType, T, SP>)producer.get(destType);
        }

        /**
         * @param type Data type that should be converted.
         * @return List of conversion paths
         */
        public Collection<ProductionPath> getAvailableProductionPaths(final String externalType) {
            final ArrayList<ProductionPath> cp = new ArrayList<>();

            if (m_producers.containsKey(externalType)) {
                for (final Entry<Class<?>, CellValueProducer<SourceType, ?, ?>> entry : m_producers.get(externalType)
                    .entrySet()) {

                    for (final JavaToDataCellConverterFactory<?> f : JavaToDataCellConverterRegistry.getInstance()
                        .getFactoriesForSourceType(entry.getKey())) {
                        cp.add(new ProductionPath(f, entry.getValue()));
                    }

                }
            }

            if (m_parent != null) {
                cp.addAll(m_parent.getAvailableProductionPaths(externalType));
            }

            return cp;
        }

        /**
         * Unregister all consumers
         *
         * @return self (for method chaining)
         */
        public ProducerRegistry<SourceType> unregisterAllProducers() {
            m_producers.clear();
            return this;
        }
    }

    /**
     * Get the {@link CellValueConsumer} registry for given destination type.
     *
     * @param destinationType {@link Destination} type for which to get the registry
     * @return Per destination type consumer registry for given destination type.
     */
    public static <DestinationType extends Destination> ConsumerRegistry<DestinationType>
        forDestinationType(final Class<DestinationType> destinationType) {

        final ConsumerRegistry<DestinationType> perDestinationType = getConsumerRegistry(destinationType);
        if (perDestinationType == null) {
            return createConsumerRegistry(destinationType);
        }

        return perDestinationType;
    }

    /**
     * Get the {@link CellValueProducer} registry for given source type.
     *
     * @param sourceType {@link Source} type for which to get the registry
     * @return Per source type producer registry for given source type.
     */
    public static <SourceType extends Source> ProducerRegistry<SourceType>
        forSourceType(final Class<SourceType> sourceType) {
        final ProducerRegistry<SourceType> perSourceType = getProducerRegistry(sourceType);
        if (perSourceType == null) {
            return createProducerRegistry(sourceType);
        }

        return perSourceType;
    }

    private static HashMap<Class<? extends Destination>, ConsumerRegistry<?>> m_destinationTypes = new HashMap<>();

    private static HashMap<Class<? extends Source>, ProducerRegistry<?>> m_sourceTypes = new HashMap<>();

    /* Get the consumer registry for given destination type */
    private static <DestinationType extends Destination> ConsumerRegistry<DestinationType>
        getConsumerRegistry(final Class<DestinationType> destinationType) {
        @SuppressWarnings("unchecked")
        final ConsumerRegistry<DestinationType> r =
            (ConsumerRegistry<DestinationType>)m_destinationTypes.get(destinationType);
        return r;
    }

    private static <SourceType extends Source> ProducerRegistry<SourceType>
        getProducerRegistry(final Class<SourceType> sourceType) {
        @SuppressWarnings("unchecked")
        final ProducerRegistry<SourceType> r = (ProducerRegistry<SourceType>)m_sourceTypes.get(sourceType);
        return r;
    }

    /* Create the consumer registry for given destination type */
    private static <DestinationType extends Destination> ConsumerRegistry<DestinationType>
        createConsumerRegistry(final Class<DestinationType> destinationType) {
        final ConsumerRegistry<DestinationType> r = new ConsumerRegistry<DestinationType>(destinationType);
        m_destinationTypes.put(destinationType, r);
        return r;
    }

    private static <SourceType extends Source> ProducerRegistry<SourceType>
        createProducerRegistry(final Class<SourceType> sourceType) {
        final ProducerRegistry<SourceType> r = new ProducerRegistry<SourceType>(sourceType);
        m_sourceTypes.put(sourceType, r);
        return r;
    }

}
