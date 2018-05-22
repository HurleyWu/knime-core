/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   May 16, 2018 (Johannes Schweig): created
 */
package org.knime.base.node.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 *
 * @author Johannes Schweig
 * @since 3.6
 */
public class JSnippetPanel2 extends JPanel {

    private JTextComponent m_expEdit;
    private KnimeCompletionProvider m_completionProvider;
    // MenuBar to add columns, flow variables and functions
    private ExpressionMenuBar m_menuBar;
    private ManipulatorProvider m_manipProvider;
    private boolean m_showColumns;
    private boolean m_showFlowVariables;

    /* Custom classes */
    /**
     * TODO
     *
     * @author jschweig
     */
    private class ButtonPlain extends JButton {

        ButtonPlain(final String s) {
            super(s);
            setFont(new Font(getFont().getName(), Font.PLAIN, getFont().getSize()));
        }

    }
    /**
     * TODO
     *
     * @author jschweig
     */
    private class LabelPlain extends JLabel {

        LabelPlain(final String s) {
            super(s);
            setFont(new Font(getFont().getName(), Font.PLAIN, getFont().getSize()));
        }

    }

    /**
     * TODO
     *
     * @author jschweig
     */
    private class MenuPlain extends JMenu {

        MenuPlain(final String s) {
            super(s);
            setFont(new Font(getFont().getName(), Font.PLAIN, getFont().getSize()));
        }

    }

    /**
     * TODO
     *
     * @author jschweig
     */
    private class MenuItemPlain extends JMenuItem {

        MenuItemPlain(final String s) {
            super(s);
            setFont(new Font(getFont().getName(), Font.PLAIN, getFont().getSize()));
            addActionListener(e -> {

            });
        }

    }

    /**
     * TODO
     */
    private class ExpressionMenuBar extends JMenuBar {

        /* Menus */
        private MenuPlain m_columnsMenu;
        private MenuPlain m_flowVarsMenu;
        private MenuPlain m_functionsMenu;
        /* Lists of icons */
        private ArrayList<Object> m_columnsList = new ArrayList<Object>();
        private ArrayList<FlowVariable> m_flowVarsList = new ArrayList<FlowVariable>();
        private ArrayList<String> m_functionsList = new ArrayList<String>();
        /* Constants for menus*/
        private static final String COLUMNS = "columns";
        private static final String FLOWVARS = "flowvars";
        private static final String FUNCTIONS = "functions";

        ExpressionMenuBar() {
            super();
            m_columnsMenu = new MenuPlain("+col");
            m_columnsMenu = new MenuPlain("+col");
            m_columnsMenu.setMnemonic(KeyEvent.VK_C);
            m_flowVarsMenu = new MenuPlain("+fvar");
            m_flowVarsMenu.setMnemonic(KeyEvent.VK_V);
            m_functionsMenu = new MenuPlain("+func");
            m_functionsMenu.setMnemonic(KeyEvent.VK_F);
            add(m_columnsMenu);
            add(m_flowVarsMenu);
            add(m_functionsMenu);
        }

        /**
         * Sets the menu entries for the columns menu
         * @param spec DataTableSpec containing the columns
         * @param list additional list with values, e.g. ROWID
         */
        private void updateColumns(final DataTableSpec spec, final String[] list) {
            m_columnsList.clear();
            m_columnsMenu.removeAll();
            for (String s : list) {
                m_columnsList.add(s);
                MenuItemPlain menuItem = new MenuItemPlain(s);
                menuItem.addActionListener(e -> onSelectionInColumnList(s));
                m_columnsMenu.add(menuItem);
            }
            for (int i = 0; i < spec.getNumColumns(); i++) {
                DataColumnSpec colSpec = spec.getColumnSpec(i);
                // TODO render colspec correctly
                m_columnsList.add(colSpec);
                MenuItemPlain menuItem = new MenuItemPlain(colSpec.toString());
                menuItem.addActionListener(e -> onSelectionInColumnList(colSpec));
                m_columnsMenu.add(menuItem);
            }

        }

        /**
         * Sets the menu entries for the flow variables menu
         * @param list list of entries to replace the current entries
         */
        private void updateFlowVars(final Collection<FlowVariable> list) {
            m_flowVarsList.clear();
            m_flowVarsMenu.removeAll();
            for (FlowVariable v : list) {
                m_flowVarsList.add(v);
                // TODO render flow variable correctly
                MenuItemPlain menuItem = new MenuItemPlain(v.toString());
                menuItem.addActionListener(e -> onSelectionInVariableList(v));
                m_flowVarsMenu.add(menuItem);
            }
        }

        /**
         * Sets the menu entries for the functions menu
         * @param list list of entries to replace the current entries
         */
        private void updateFunctions(final ArrayList<String> list) {
            m_functionsList.clear();
            m_functionsMenu.removeAll();
            for (String s : list) {
                m_functionsList.add(s);
                MenuItemPlain menuItem = new MenuItemPlain(s);
                menuItem.addActionListener(e -> onSelectionInManipulatorList(s));
                m_functionsMenu.add(menuItem);
            }
        }

        /**
         * @return
         */
        public ArrayList<FlowVariable> getFlowVarsList() {
            return m_flowVarsList;
        }

        /**
         * @return
         */
        public ArrayList<Object> getColList() {
            return m_columnsList;
        }

    }

    /**
     * @see JSnippetPanel
     */
    public JSnippetPanel2(final ManipulatorProvider manipulatorProvider,
        final KnimeCompletionProvider completionProvider) {
        this(manipulatorProvider, completionProvider, true);
    }

    /**
     * @see JSnippetPanel
     */
    public JSnippetPanel2(final ManipulatorProvider manipulatorProvider,
        final KnimeCompletionProvider completionProvider, final boolean showColumns) {
        this(manipulatorProvider, completionProvider, showColumns, true);
    }

    /**
     * @see JSnippetPanel
     */
    public JSnippetPanel2(final ManipulatorProvider manipulatorProvider, final KnimeCompletionProvider completionProvider,
        final boolean showColumns, final boolean showFlowVariables) {
        m_manipProvider = manipulatorProvider;
        m_completionProvider = completionProvider;
        m_showColumns = showColumns;
        m_showFlowVariables = showFlowVariables;
        setLayout(new BorderLayout());

        /* Init GUI */
        JPanel editorPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.gridwidth = 3;
        c.insets = new Insets(4, 0, 4, 0);
        editorPanel.add(getFontPlainLabel(new JLabel("Editor")), c);
        c.weightx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 4, 4);
        /* Menubar */
        m_menuBar = new ExpressionMenuBar();
        editorPanel.add(m_menuBar, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 8, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        m_expEdit = createEditorComponent();
        m_expEdit.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        m_expEdit.setPreferredSize(new Dimension(m_expEdit.getPreferredSize().width, 100));
        editorPanel.add(m_expEdit, c);
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        editorPanel.add(getFontPlainLabel(new JLabel("Evaluate on first row")), c);
        c.gridy++;
        editorPanel.add(getFontPlainLabel(new JLabel("...")), c);
        c.gridx++;
        editorPanel.add(getFontPlainButton(new JButton("Evaluate")), c);


        add(editorPanel, BorderLayout.CENTER);
    }

    /**
     * Returns the expression in the editor.
     *
     * @return a string containing the expression
     */
    public String getExpression() {
        return m_expEdit.getText();
    }

    /**
     * Method that is being called by listener when an object in the column menu has been selected.
     *
     * @param selected The selected object.
     * @since 3.6
     */
    protected void onSelectionInColumnList(final Object selected) {
        String enter;
        if (selected instanceof String) {
            enter = "$$" + selected + "$$";
        } else {
            DataColumnSpec colSpec = (DataColumnSpec)selected;
            String name = colSpec.getName().replace("$", "\\$");
            enter = m_completionProvider.escapeColumnName(name);
        }
        m_expEdit.replaceSelection(enter);
            m_expEdit.requestFocus();
    }

    /**
     * Method that is being called by listener when an object in the variable list has been selected.
     *
     * @param selected The selected object.
     * @since 3.6
     */

    protected void onSelectionInVariableList(final Object selected) {
        if (selected instanceof FlowVariable) {
            FlowVariable v = (FlowVariable)selected;
            String typeChar;
            switch (v.getType()) {
                case DOUBLE:
                    typeChar = "D";
                    break;
                case INTEGER:
                    typeChar = "I";
                    break;
                case STRING:
                    typeChar = "S";
                    break;
                default:
                    return;
            }
            String enter =
                    m_completionProvider.escapeFlowVariableName(typeChar
                            + v.getName()/*.replace("\\", "\\\\").replace("}", "\\}")*/);
            m_expEdit.replaceSelection(enter);
            m_expEdit.requestFocus();
        }

    }    /**
     * Inserts text based on the selected manipulator.
     *
     * @param selected A {@link Manipulator}.
     * @since 2.8
     */
    protected void onSelectionInManipulatorList(final Object selected) {
        Manipulator manipulator = (Manipulator)selected;
        String selectedString = m_expEdit.getSelectedText();
        StringBuilder newStr = new StringBuilder(manipulator.getName());
        newStr.append('(');
        for (int i = 0; i < manipulator.getNrArgs(); i++) {
            newStr.append(i > 0 ? ", " : "");
            if (i == 0 && selectedString != null) {
                newStr.append(selectedString);
            }
        }
        newStr.append(')');

        m_expEdit.replaceSelection(newStr.toString());
        if (manipulator.getNrArgs() > 0 && selectedString == null) {
            int caretPos = m_expEdit.getCaretPosition();
            m_expEdit.setCaretPosition(1 + m_expEdit.getText().indexOf('(',
                caretPos - newStr.toString().length()));
        }

            m_expEdit.requestFocus();
    }

    public void update(final String expression, final DataTableSpec spec, final Map<String, FlowVariable> flowVariables) {
        // we have Expression.VERSION_2X
        final String[] expressions = new String[] {Expression.ROWID, Expression.ROWINDEX, Expression.ROWCOUNT};
        update(expression, spec, flowVariables, expressions);

    }
    /**
     * @param string
     * @param specs
     * @param flowVariables
     */
    public void update(final String expression, final DataTableSpec spec, final Map<String, FlowVariable> flowVariables, final String[] expressions) {
        m_expEdit.setText(expression);
        m_expEdit.requestFocus();

        m_menuBar.updateColumns(spec, expressions);
        m_menuBar.updateFlowVars(flowVariables.values());

        m_completionProvider.setColumns(spec);
        m_completionProvider.setFlowVariables(flowVariables.values());
    }

    /**
     * @param string
     */
    public void setExpressions(final String expression) {
        m_expEdit.setText(expression);
        m_expEdit.requestFocus();
    }

    /**
     * @param expEdit the text editor to set
     * @since 2.8
     */
    protected void setExpEdit(final JTextComponent expEdit) {
        this.m_expEdit = expEdit;
    }

    /**
     * @return the completionProvider
     * @since 2.8
     */
    protected KnimeCompletionProvider getCompletionProvider() {
        return m_completionProvider;
    }

    /**
     * @return the flowVarsList
     * @since 2.10
     */
    protected ArrayList<FlowVariable> getFlowVarsList() {
        return m_menuBar.getFlowVarsList();
    }

    /**
     * @return the expEdit
     * @since 2.10
     */
    protected JTextComponent getExpEdit() {
        return m_expEdit;
    }

    /**
     * @return the colList
     * @since 2.10
     */
    protected ArrayList<Object>  getColList() {
        return m_menuBar.getColList();
    }
/**
	 * Sets the font weight of the passed component to plain.
	 * @param c a passed component
	 * @return the component with plain font weight
	 */
	private static JComponent getFontPlain(final JComponent c) {
		c.setFont(new Font(c.getFont().getName(), Font.PLAIN, c.getFont().getSize()));
        return c;
    }

	/**
	 * @see {@link getFontPlain}
	 */
	private static JButton getFontPlainButton(final JButton b) {
	    return (JButton) getFontPlain(b);
	}

	/**
	 * @see {@link getFontPlain}
	 */
	private static JLabel getFontPlainLabel(final JLabel l) {
	    return (JLabel) getFontPlain(l);
	}

/**
     * Creates the text editor component along with the scrollpane.
     *
     * @return The {@link RSyntaxTextArea} wrapped within a {@link JScrollPane}.
     * @since 2.8
     */
    protected JTextComponent createEditorComponent() {
        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        JScrollPane scroller = new JScrollPane(textArea);

        // An AutoCompletion acts as a "middle-man" between a text component
        // and a CompletionProvider. It manages any options associated with
        // the auto-completion (the popup trigger key, whether to display a
        // documentation window along with completion choices, etc.). Unlike
        // CompletionProviders, instances of AutoCompletion cannot be shared
        // among multiple text components.
        AutoCompletion ac = new AutoCompletion(m_completionProvider);
        ac.setShowDescWindow(true);

        ac.install(textArea);

        setExpEdit(textArea);
        return textArea;
    }
}
