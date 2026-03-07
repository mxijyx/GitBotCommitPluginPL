package com.frshaka.gitbot.settings

import com.frshaka.gitbot.ai.OpenRouterClient
import com.frshaka.gitbot.prompt.PromptLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class GitBotConfigurable : Configurable {

    private var panel: JPanel? = null

    private val apiKeyField = JPasswordField()

    // Lista mutável de modelos disponíveis; começa vazia e é preenchida de forma assíncrona
    private val availableModels = mutableListOf<String>()

    // Campo editável: usuário pode digitar o ID do modelo manualmente caso a lista ainda não tenha carregado
    private val modelField = ComboBox<String>().apply { isEditable = true }

    private val languageCombo = ComboBox(arrayOf("PT_BR", "EN", "PL"))

    private val promptArea = JTextArea(14, 60).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    private val resetButton = JButton("Reset to default")

    // Botão para carregar a lista de modelos do OpenRouter de forma assíncrona
    private val loadModelsButton = JButton("Load Models")

    override fun getDisplayName(): String = "GitBot Commit"

    override fun createComponent(): JComponent {
        val root = JPanel(BorderLayout())
        val form = JPanel(GridBagLayout())

        // Aviso exibido quando o PasswordSafe está em modo memória (keyring do sistema indisponível).
        // Nesse caso a API key não é persistida entre sessões da IDE.
        // O texto é exibido no idioma configurado no plugin.
        if (GitBotSecrets.isMemoryOnly()) {
            val lang = GitBotSettingsService.getInstance().state.language
            val warningText = when (lang) {
                "PT_BR" -> {
                    "<html>" +
                            "<b>⚠ Atenção: a API Key não será salva entre sessões da IDE.</b><br/><br/>" +
                            "O cofre de senhas do IntelliJ está operando apenas em memória porque o " +
                            "gerenciador de credenciais do sistema não está disponível.<br/><br/>" +
                            "<b>Como corrigir:</b><br/>" +
                            "• <b>Windows:</b> verifique se o <i>Windows Credential Manager</i> está ativo " +
                            "(<i>Painel de Controle → Gerenciador de Credenciais</i>).<br/>" +
                            "• <b>Linux:</b> instale e inicie o <i>KWallet</i> ou <i>GNOME Keyring (SecretService)</i> " +
                            "e certifique-se de que o daemon está em execução antes de abrir o IntelliJ.<br/>" +
                            "• <b>macOS:</b> verifique se o <i>Keychain Access</i> está desbloqueado.<br/><br/>" +
                            "Após corrigir, reinicie o IntelliJ e reconfigure a API Key." +
                            "</html>"
                }
                "PL" -> {
                    "<html>" +
                            "<b>⚠ Ostrzeżenie: Twój klucz API nie zostanie zapisany między sesjami IDE.</b><br/><br/>" +
                            "Sejf haseł IntelliJ działa w trybie tylko pamięci, ponieważ systemowy menedżer poświadczeń " +
                            "jest niedostępny.<br/><br/>" +
                            "<b>Jak naprawić:</b><br/>" +
                            "• <b>Windows:</b> upewnij się, że <i>Menedżer poświadczeń systemu Windows</i> jest włączony " +
                            "(<i>Panel sterowania → Menedżer poświadczeń</i>).<br/>" +
                            "• <b>Linux:</b> zainstaluj i uruchom <i>KWallet</i> lub <i>GNOME Keyring (SecretService)</i> " +
                            "i upewnij się, że demon jest uruchomiony przed uruchomieniem IntelliJ.<br/>" +
                            "• <b>macOS:</b> upewnij się, że <i>Keychain Dostęp</i> jest odblokowany.<br/><br/>" +
                            "Po naprawieniu, uruchom ponownie IntelliJ i ponownie wprowadź klucz API." +
                            "</html>"
                }
                else -> {
                    "<html>" +
                            "<b>⚠ Warning: your API Key will not be saved between IDE sessions.</b><br/><br/>" +
                            "IntelliJ's password safe is running in memory-only mode because the system " +
                            "credential manager is not available.<br/><br/>" +
                            "<b>How to fix:</b><br/>" +
                            "• <b>Windows:</b> make sure <i>Windows Credential Manager</i> is enabled " +
                            "(<i>Control Panel → Credential Manager</i>).<br/>" +
                            "• <b>Linux:</b> install and start <i>KWallet</i> or <i>GNOME Keyring (SecretService)</i> " +
                            "and ensure the daemon is running before launching IntelliJ.<br/>" +
                            "• <b>macOS:</b> make sure <i>Keychain Access</i> is unlocked.<br/><br/>" +
                            "After fixing, restart IntelliJ and re-enter your API Key." +
                            "</html>"
                }
            }
            val warning = JLabel(warningText).apply {
                foreground = java.awt.Color(180, 80, 0)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(java.awt.Color(200, 120, 0)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
                )
            }
            root.add(warning, BorderLayout.NORTH)
        }

        val c = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(6, 6, 6, 6)
        }

        fun row(y: Int, label: String, comp: JComponent) {
            c.gridy = y

            c.gridx = 0
            c.weightx = 0.0
            form.add(JBLabel(label), c)

            c.gridx = 1
            c.weightx = 1.0
            form.add(comp, c)
        }

        row(0, "OpenRouter API Key:", apiKeyField)

        // Linha do modelo: combo + botão "Load Models" lado a lado
        c.gridy = 1
        c.gridx = 0
        c.weightx = 0.0
        form.add(JBLabel("Model:"), c)

        c.gridx = 1
        c.weightx = 1.0
        val modelRow = JPanel(BorderLayout(4, 0)).apply {
            add(modelField, BorderLayout.CENTER)
            add(loadModelsButton, BorderLayout.EAST)
        }
        form.add(modelRow, c)

        row(2, "Commit language:", languageCombo)

        // Editor de prompt
        c.gridy = 3
        c.gridx = 0
        c.weightx = 0.0
        c.anchor = GridBagConstraints.NORTHWEST
        form.add(JBLabel("Prompt template:"), c)

        c.gridx = 1
        c.weightx = 1.0
        c.fill = GridBagConstraints.BOTH
        val promptScroll = JBScrollPane(promptArea).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
        form.add(promptScroll, c)

        // Botão de reset abaixo do prompt
        c.gridy = 4
        c.gridx = 1
        c.weightx = 1.0
        c.fill = GridBagConstraints.NONE
        c.anchor = GridBagConstraints.WEST
        form.add(resetButton, c)

        // Listeners
        resetButton.addActionListener {
            val lang = languageCombo.selectedItem as String
            promptArea.text = loadDefaultPrompt(lang)
        }

        languageCombo.addActionListener {
            // Ao trocar o idioma, exibe o prompt salvo correspondente
            val settings = GitBotSettingsService.getInstance().state
            val lang = languageCombo.selectedItem as String
            ensureDefaultsLoaded(settings)

            promptArea.text = if (lang == "PT_BR") settings.promptPtBr else if (lang == "PL") settings.promptPl else settings.promptEn
        }

        // Carrega modelos assincronamente ao clicar no botão
        loadModelsButton.addActionListener {
            loadModelsAsync()
        }

        // Configura o campo de modelo com popup de busca ao clicar
        configureModelField()
        root.add(form, BorderLayout.CENTER)

        panel = root
        reset()
        return root
    }

    override fun isModified(): Boolean {
        val settings = GitBotSettingsService.getInstance().state
        ensureDefaultsLoaded(settings)

        val savedKey = GitBotSecrets.getApiKey() ?: ""

        val uiKey = String(apiKeyField.password).trim()
        val uiModel = (modelField.editor.item as? String)?.trim() ?: (modelField.selectedItem as? String) ?: ""
        val uiLang = languageCombo.selectedItem as String
        val uiPrompt = promptArea.text

        val currentSavedPrompt = if (uiLang == "PL") settings.promptPl else if (uiLang == "PT_BR") settings.promptPtBr else settings.promptEn

        return uiKey != savedKey ||
                uiModel != settings.model ||
                uiLang != settings.language ||
                uiPrompt != currentSavedPrompt
    }

    override fun apply() {
        val settings = GitBotSettingsService.getInstance().state
        ensureDefaultsLoaded(settings)

        val uiKey = String(apiKeyField.password).trim()
        val uiModel = (modelField.editor.item as? String)?.trim() ?: (modelField.selectedItem as? String) ?: ""
        val uiLang = languageCombo.selectedItem as String
        val uiPrompt = promptArea.text

        if (uiKey.isNotEmpty()) {
            GitBotSecrets.setApiKey(uiKey)
        }

        settings.model = uiModel
        settings.language = uiLang

        when (uiLang) {
            "PT_BR" -> {
                settings.promptPtBr = uiPrompt
            }
            "PL" -> {
                settings.promptPl = uiPrompt
            }
            else -> {
                settings.promptEn = uiPrompt

            }
        }
    }

    override fun reset() {
        val settings = GitBotSettingsService.getInstance().state
        ensureDefaultsLoaded(settings)

        val savedKey = GitBotSecrets.getApiKey() ?: ""

        apiKeyField.text = savedKey
        languageCombo.selectedItem = settings.language
        promptArea.text = if (settings.language == "PT_BR") settings.promptPtBr else if (settings.language == "PL") settings.promptPl else settings.promptEn

        // Preenche o modelo salvo no campo editável
        setModelFieldValue(settings.model)

        // Se já existe API key salva, carrega a lista de modelos em background automaticamente
        if (savedKey.isNotEmpty()) {
            loadModelsAsync()
        }
    }

    override fun disposeUIResources() {
        panel = null
    }

    /**
     * Define o valor do campo de modelo, garantindo que o item esteja na lista
     * ou simplesmente define o texto quando a lista ainda não foi carregada.
     */
    private fun setModelFieldValue(modelName: String) {
        if (modelName.isBlank()) return
        ensureModelInList(modelName)
        modelField.selectedItem = modelName
    }

    private fun ensureModelInList(modelName: String) {
        if (modelName.isBlank()) return
        if (availableModels.none { it == modelName }) {
            availableModels += modelName
            availableModels.sort()
            // Sincroniza o ComboBox com a lista atualizada sem perder a seleção atual
            val current = (modelField.editor.item as? String) ?: (modelField.selectedItem as? String)
            modelField.removeAllItems()
            availableModels.forEach { modelField.addItem(it) }
            modelField.selectedItem = current
        }
    }

    /**
     * Busca a lista de modelos do OpenRouter em uma thread de background.
     * Deve ser chamada após o usuário ter configurado a API key.
     * Atualiza o ComboBox na EDT ao terminar.
     */
    private fun loadModelsAsync() {
        val apiKey = String(apiKeyField.password).trim().ifEmpty {
            GitBotSecrets.getApiKey() ?: ""
        }

        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(
                panel,
                "Informe a OpenRouter API Key antes de carregar os modelos.",
                "API Key ausente",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        loadModelsButton.isEnabled = false
        loadModelsButton.text = "Loading..."

        ApplicationManager.getApplication().executeOnPooledThread {
            val models = try {
                val client = OpenRouterClient(apiKey)
                client.models().map { it.id }.sorted()
            } catch (e: Exception) {
                emptyList()
            }

            // Atualiza a UI na EDT
            SwingUtilities.invokeLater {
                loadModelsButton.isEnabled = true
                loadModelsButton.text = "Load Models"

                if (models.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        panel,
                        "Não foi possível carregar os modelos. Verifique a API Key e a conexão.",
                        "Erro ao carregar modelos",
                        JOptionPane.ERROR_MESSAGE
                    )
                    return@invokeLater
                }

                // Preserva o modelo atualmente selecionado/digitado
                val current = (modelField.editor.item as? String)?.trim()
                    ?: (modelField.selectedItem as? String)?.trim()
                    ?: ""

                availableModels.clear()
                availableModels.addAll(models)

                modelField.removeAllItems()
                availableModels.forEach { modelField.addItem(it) }

                // Reaplica a seleção anterior se ainda válida, caso contrário mantém como texto editável
                if (current.isNotEmpty()) {
                    modelField.selectedItem = current
                    if (modelField.selectedItem != current) {
                        modelField.editor.item = current
                    }
                }
            }
        }
    }

    private fun configureModelField() {
        // Intercept mouse clicks para exibir popup com busca (somente quando há modelos carregados)
        modelField.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                if (availableModels.isNotEmpty()) {
                    e.consume()
                    showSearchablePopup()
                }
                // Se a lista estiver vazia, o campo editável padrão do ComboBox é usado normalmente
            }
        })
    }

    private fun showSearchablePopup() {
        val searchField = JTextField(20)
        val listModel = DefaultListModel<String>()
        availableModels.forEach { listModel.addElement(it) }

        val currentValue = (modelField.editor.item as? String) ?: (modelField.selectedItem as? String) ?: ""

        val list = JBList(listModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            setSelectedValue(currentValue, true)
        }

        // Ajusta largura do popup à largura do combo, com mínimo de 400px
        val comboWidth = modelField.width.coerceAtLeast(400)
        val popupHeight = 400

        val scrollPane = JBScrollPane(list)
        scrollPane.preferredSize = java.awt.Dimension(comboWidth, popupHeight)

        val panel = JPanel(BorderLayout()).apply {
            add(searchField, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
            preferredSize = java.awt.Dimension(comboWidth, popupHeight + 30) // +30 para o campo de busca
        }

        // Filtra a lista conforme o usuário digita
        searchField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = filterList()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = filterList()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = filterList()

            private fun filterList() {
                val filter = searchField.text.trim()
                listModel.clear()
                availableModels
                    .filter { it.contains(filter, ignoreCase = true) }
                    .forEach { listModel.addElement(it) }
            }
        })

        val popup = com.intellij.openapi.ui.popup.JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, searchField)
            .setTitle("Select Model")
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .createPopup()

        // Seleciona o modelo ao clicar na lista
        list.addListSelectionListener {
            if (!it.valueIsAdjusting && list.selectedValue != null) {
                modelField.selectedItem = list.selectedValue
                popup.closeOk(null)
            }
        }

        popup.showUnderneathOf(modelField)
    }

    private fun ensureDefaultsLoaded(settings: GitBotSettingsState) {
        if (settings.promptPtBr.isBlank()) {
            settings.promptPtBr = loadDefaultPrompt("PT_BR")
        }
        if (settings.promptEn.isBlank()) {
            settings.promptEn = loadDefaultPrompt("EN")
        }
        if (settings.promptPl.isBlank()) {
            settings.promptPl = loadDefaultPrompt("PL")
        }
    }

    private fun loadDefaultPrompt(lang: String): String {
        return when (lang) {
            "PT_BR" -> {
                PromptLoader.load("prompts/commit_prompt_ptbr.txt")
            }
            "PL" -> {
                PromptLoader.load("prompts/commit_prompt_pl.txt")
            }
            else -> {
                PromptLoader.load("prompts/commit_prompt_en.txt")
            }
        }
    }
}
