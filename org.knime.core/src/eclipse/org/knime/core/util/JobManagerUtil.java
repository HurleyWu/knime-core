/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 22, 2016 (hornm): created
 */
package org.knime.core.util;

import org.knime.core.api.node.workflow.JobManagerUID;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManagerFactory;

/**
 * Utility class for {@link NodeExecutionJobManager}-functions (e.g. conversion).
 *
 * @author Martin Horn, KNIME.com
 */
public class JobManagerUtil {

    private JobManagerUtil() {
        // utility class
    }

    /**
     * Maps the given job manager uid (unique identifier) to the respective {@link NodeExecutionJobManagerFactory}.
     *
     * @param uid the unique identifier for a job manager
     * @return the associated {@link NodeExecutionJobManagerFactory}
     */
    public static NodeExecutionJobManagerFactory getJobManagerFactory(final JobManagerUID uid) {
        return NodeExecutionJobManagerPool.getJobManagerFactory(uid.getID());
    }

    /**
     * Maps a {@link NodeExecutionJobManagerFactory} to a unique job manager identifier ({@link JobManagerUID}).
     *
     * @param factory the factory to map
     * @return a newly created instance of {@link JobManagerUID}
     */
    public static JobManagerUID getJobManagerUID(final NodeExecutionJobManagerFactory factory) {
        return JobManagerUID.builder(factory.getID()).setName(factory.getLabel()).build();
    }

    /**
     * Maps a {@link NodeExecutionJobManager} to a unique job manager identifier.
     * @param jobManager the job manager to map
     * @return a newly created instance of {@link JobManagerUID} with its id taken from the argument job manager (and nothing else)
     */
    public static JobManagerUID getJobManagerUID(final NodeExecutionJobManager jobManager) {
        return JobManagerUID.builder(jobManager.getID()).build();
    }
}
