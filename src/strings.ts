export const STRINGS = {
  // Shared button captions.
  common: {
    back: 'Back',
    next: 'Next',
    done: 'Done',
    skip: 'Skip',
    cancel: 'Cancel',
    add: 'Add',
    save: 'Save',
    close: 'Close',
    reset: 'Reset',
  },

  // Banners.
  banners: {
    // No installed voice for language.
    ttsMissingVoice: 'No installed voice for {name} ({code}). Speaking with the default voice.',
    // Platform-specific guidance.
    ttsInstallAndroid: " Tap to open your device's speech settings, then install voice data for this language.",
    ttsInstallIos: ' To add it, open Settings → Accessibility → Spoken Content → Voices and download the voice. Tap to dismiss.',
    ttsInstallWeb: ' Voices come from your operating system — add one for this language in your system speech settings. Tap to dismiss.',
    // Newer build deployed.
    updateAvailable: 'Update available — tap to reload',
  },

  // Tutorial walkthrough.
  tutorial: {
    slides: [
      {
        title: 'Welcome to SpeGen',
        body: 'This is a short tutorial to teach you the basic UI of the app. You can access this tutorial later from Settings.',
      },
      {
        title: 'Folders',
        body: 'Tap a folder to go to another menu with more symbols and folders. Folders always have a black fold in the top right corner.',
      },
      {
        title: 'Symbols',
        body: 'Tap a symbol to add it to the input box.',
      },
      {
        title: 'Input Box',
        body: 'The input box is where you compose sentences. Tap it to play the constructed sentence.',
      },
      {
        title: 'Menu Row',
        body: 'These are buttons that automatically redirect you to the associated menu.',
      },
      {
        title: 'Static Words Row',
        body: 'These terms are always at the bottom of the screen no matter where you are. Tap one to instantly speak it.',
      },
      {
        title: 'Keyboard',
        body: "Opens a dialog that lets you use your device's keyboard to add items to the input box.",
      },
      {
        title: 'Delete',
        body: 'Deletes the last term in the input box.',
      },
      {
        title: 'Clear',
        body: 'Clears the input box of all terms.',
      },
      {
        title: 'Stop',
        body: 'Stops any currently playing text-to-speech.',
      },
      {
        title: 'Search',
        body: 'Search for any term in any menu. It guides you to the term by highlighting the buttons you need to press.',
      },
      {
        title: 'Settings',
        body: 'Opens the settings menu: editing the board, backing up data, changing the text-to-speech voice, and more.',
      },
      {
        title: 'Back',
        body: 'Sends you back to the previous menu you were on.',
      },
      {
        title: 'Autocomplete',
        body: 'Replaces the menu with a list of autocomplete options that learn from the symbols you select. Data never leaves your device without consent.',
      },
    ],
  },

  // Word finder (WordFinder.tsx + WordFinderGuide).
  wordFinder: {
    title: 'Find a word/item',
    subtitle: 'Search every menu, then click the highlighted buttons to navigate to the word.',
    placeholder: 'Type a word…',
    noMatches: 'No matches.',
    find: 'Find',
    close: 'Close',
    finding: 'Finding “{word}” - tap the highlighted button',
    cancel: 'Cancel',
  },

  // Autocomplete (next-word prediction) menu.
  autocomplete: {
    suggestions: 'Suggestions',
    after: 'After "{word}"',
    none: 'No suggestions yet.',
  },

  // Settings screen grouped by tab.
  settings: {
    title: 'Settings',
    apply: 'Apply',
    done: 'Done',
    tabs: {
      display: 'Display',
      voice: 'Voice',
      vocabulary: 'Vocabulary',
      language: 'Language',
      data: 'Data',
      about: 'About',
      feedback: 'Feedback',
    },

    display: {
      preview: 'Preview',
      staticRowLabel: 'Static words row',
      menuRowLabel: 'Menu row',
      previewStaticTerms: ['Yes', 'No', 'More'],
      previewMenuTitles: ['People', 'Food', 'Actions'],
      itemSizing: 'Item sizing',
      itemWidthSize: 'Item width size',
      itemHeightSize: 'Item height size',
      gridSize: 'Fit to grid',
      gridHint: 'Scale item size to fit a grid: enter columns and rows, then tap Apply.',
      gridCols: 'Cols',
      gridRows: 'Rows',
      gridApply: 'Apply',
      bordersSpacing: 'Borders & spacing',
      borderWidth: 'Border width',
      itemPadding: 'Item padding',
      staticRowStyle: 'Static words row style',
      menuRowStyle: 'Menu row style',
      textSize: 'Text size',
      textPadding: 'Text padding',
      buttonShape: 'Button shape',
      labelPosition: 'Label position',
      top: 'Top',
      bottom: 'Bottom',
      skinTone: 'Skin tone',
      theme: 'Theme',
      themeLight: 'Light',
      themeDark: 'Dark',
      themeSystem: 'System',
      themeNote:
        "Dark mode themes the app's screens and menus. Symbol button colours and high-contrast mode are unaffected.",
      highContrast: 'High contrast',
      highContrastHint: 'Black background, white borders and labels with simplified color palette for item images.',
    },

    voice: {
      speechRate: 'Speech rate',
      pitch: 'Pitch',
      pauseBetweenWords: 'Pause between words',
      pauseDuration: 'Pause duration',
      testPhrase: 'This is a test of the voice settings.',
      testVoice: 'Test voice',
      translateSentence: 'Translate spoken sentences',
      translateSentenceHint:
        'Translate the whole sentence at once for more natural grammar, instead of word by word. On by default for non-English languages. Needs internet.',
      voiceLabel: 'Voice',
      voiceHint: 'Choose the voice used for speech in your app language. Voices are provided by your device or browser.',
      voiceDefault: 'Default (system)',
      noVoices: 'No voices found for this language.',
      openDeviceTts: 'Open device TTS settings',
      ttsOpenFailed: 'Could not open device TTS settings: {e}',
      resetToDefaults: 'Reset to defaults',
      resetDone: 'Voice reset to defaults.',
    },

    vocabulary: {
      editorMode: 'Editor mode',
      editorModeHint: 'Edit symbols and folders by tapping them on the board and use the buttons on the toolbar to edit menus.',
      enableEditorMode: 'Enable editor mode',
      staticRow: 'Static words row',
      staticRowHint: 'Words always visible at the bottom of the screen reguardless of where you are in the app. Drag ≡ to reorder.',
      addPhrasePlaceholder: 'Add a phrase',
      addPhrase: 'Add',
      menuRow: 'Menu row',
      menuRowHint: 'Menu shortcuts shown just above the static row when using the board and main menu. Drag ≡ to reorder.',
      menuRowEmpty: 'No menus in the row.',
      addMenuToRow: 'Add menu to row',
      allMenusInRow: 'All menus are already in the row.',
    },

    language: {
      appLanguage: 'App language',
      appLanguageHint: 'Used for speech, image search, and label translation. Changing this reloads all images.',
      translationDisclaimer:
        'App text is translated with the Google Translate API, so some translations may be inaccurate. If you spot an error, please use the Feedback button in Settings to report it — include the language and the suggested correction.',
      autoTranslate: 'Automatic translation',
      translateUi: 'Translate app interface',
      translateUiHint: 'Show menus, buttons and settings in the app language.',
      translateLabels: 'Translate board & menu labels',
      translateLabelsHint:
        'Auto-translate symbol labels, menu names and the static words row into the app language. Needs internet.',
      multilingual: 'Multilingual',
      perItemImageLanguage: 'Per-item image language',
      perItemImageLanguageHint: 'Let each item search for images in its own language.',
      multilingualLabels: 'Multilingual labels',
      multilingualLabelsHint:
        'Add extra text labels per item (in edit mode) for other languages. While enabled, only board item labels stop auto-translating when you switch language (your manual labels are used instead); the static words row and menu names still auto-translate.',
      spokenLanguages: 'Spoken languages',
      spokenLanguagesHint:
        'Languages you can switch between from the input box. The app language is the base; items without a manual label are auto-translated for speech. This feature needs internet and is skipped for English.',
      base: 'Base',
      addLanguage: 'Add language',
    },

    data: {
      backupRestore: 'Backup & restore',
      backupRestoreHint:
        'Export or import all boards, settings, custom images and recorded audio as a single backup file you can transfer to another device or keep in your device storage.',
      downloadBackup: 'Download backup',
      exportBackup: 'Export backup',
      saveBackupToDevice: 'Save backup to device (choose folder)',
      importBackup: 'Import backup',
      security: 'Security',
      securityHint:
        "Require a PIN to open Settings and the editor. This can be used to ensure that the user can't edit settings without the PIN when in the main interface. Saved when you tap Apply or Done.",
      changePin: 'Change PIN',
      removePin: 'Remove PIN',
      setPin: 'Set a PIN',
      recoveryQuestions: 'Recovery questions',
      recoveryQuestionsHint:
        'Used to reset a forgotten PIN. It is recommended to add at least one. Without any, a forgotten PIN can only be cleared by reinstalling the app.',
      question: 'Question',
      ownQuestionPlaceholder: 'Or type your own question',
      answer: 'Answer',
      addQuestion: '+ Add question',
      pinPlaceholder: 'PIN (min 4 digits)',
      confirmPinPlaceholder: 'Confirm PIN',
      pinTooShort: 'PIN must be at least 4 digits.',
      pinsDoNotMatch: 'PINs do not match.',
      reset: 'Reset',
      restoreDefaults: 'Restore defaults',
      backupFormatNote:
        'The .spegen backup is a compressed ZIP holding your boards, settings, custom images and recorded audio in one file. Importing replaces all current boards and settings.',
      // Status messages.
      preparingDownload: 'Preparing download…',
      backupDownloaded: 'Backup downloaded.',
      openingShareSheet: 'Opening share sheet…',
      shareSheetHint: 'Use the share sheet to save your backup.',
      sharingUnavailable: 'Sharing is not available.',
      chooseFolder: 'Choose a folder to save into…',
      savedToFolder: 'Saved to the folder you chose.',
      saveCanceled: 'Save canceled.',
      saveFailed: 'Save failed: {e}',
      chooseBackupFile: 'Choose a backup file…',
      importCanceled: 'Import canceled.',
      importWebFileMissing: 'Import failed: Web file handle is missing.',
      importDone: 'Imported. Board reloaded.',
      importFailed: 'Import failed: {e}',
      confirmReset: 'Tap "Restore defaults" again to confirm. Warning: this erases your boards.',
      resetDone: 'Restored default boards.',
      resetFailed: 'Reset failed: {e}',
    },

    about: {
      appName: 'SpeGen',
      intro: "An open-source AAC app developed by Harper Klein Keane. SpeGen's GitHub page can be found at ",
      githubLabel: 'hkleinkeane.github.io/spegen',
      githubUrl: 'https://hkleinkeane.github.io/spegen/',
      websiteIntro: ', and the website can be found at ',
      websiteLabel: 'spegen.vercel.app',
      websiteUrl: 'https://spegen.vercel.app',
      end: '.',
      license: 'License: GNU General Public License v3.0',
      showTutorialAgain: 'Show the welcome tutorial again',
      privacyPolicy: 'Privacy Policy',
    },

    feedback: {
      title: 'Submit Feedback',
      intro:
        "Found a bug or have a feature request? Let me know! This form will automatically open a tracking issue on Spegen's GitHub repository.",
      category: 'Issue Category',
      categories: ['Bug', 'Suggestion', 'Other'],
      header: 'Header / Summary',
      headerPlaceholder: 'Briefly summarize the issue',
      details: 'Details / Body',
      detailsPlaceholder: 'Provide steps to reproduce bug or expand on your feature idea...',
      submitting: 'Submitting...',
      submit: 'Submit Feedback',
      missingFields: 'Please provide both a header and full details.',
      submittingStatus: 'Submitting feedback...',
      submitted: 'Feedback successfully submitted! Thank you.',
      submitFailed: 'Failed to submit issue: {msg}',
      networkError: 'Network error submitting feedback: {e}',
    },
  },

  // The eight button-box actions around the board and the keyboard "Add text" dialog.
  board: {
    settings: 'Settings',
    search: 'Search',
    stop: 'Stop',
    keyboard: 'Keyboard',
    delete: 'Delete',
    clear: 'Clear',
    back: 'Back',
    autocomplete: 'Autocomplete',
    addText: 'Add text',
  },

  // Editor-mode toolbar.
  editor: {
    title: 'EDITOR MODE',
    addItem: '+ Item',
    addMenu: '+ Menu',
    deleteMenu: '- Menu',
    gotoMenu: 'Go To Menu',
    applyChanges: 'Apply Changes',
    exit: 'Exit',
  },

  // Reusable language picker.
  languageDropdown: {
    select: 'Select…',
    searchPlaceholder: 'Search language',
  },

  // Editor / utility dialogs.
  dialogs: {
    // Shared TTS-behaviour radio labels (Add item + Edit item). Index = the stored tts mode (0/1/2).
    ttsTypeLabels: ['Type only', 'Speak only', 'Both'],

    addItem: {
      title: 'Add item to {menuTitle}',
      name: 'Name',
      symbol: 'Symbol',
      folder: 'Folder',
      folderTargetMenu: 'Folder target menu',
    },

    newMenu: {
      title: 'Create new menu',
      menuTitle: 'Menu title',
      emptyNote: 'The menu is created empty. Enter it and use "+ Item" to add items.',
      create: 'Create',
    },

    deleteMenu: {
      title: 'Delete a menu',
      hint: 'Folders that open a deleted menu will also be removed.',
      delete: 'Delete',
    },

    gotoMenu: {
      title: 'Go to a menu',
      hint: 'Lets you jump to a menu while in editor mode.',
      goTo: 'Go To',
    },

    unsaved: {
      title: 'Unsaved Changes',
      body: 'You have unsaved changes. Do you want to save them?',
      dontSave: "Don't Save",
      saveChanges: 'Save Changes',
    },

    pinLock: {
      enterTitle: 'Enter PIN',
      lockedHint: 'Settings and the editor are locked.',
      pinPlaceholder: 'PIN',
      unlock: 'Unlock',
      forgotPin: 'Forgot PIN?',
      incorrectPin: 'Incorrect PIN.',
      noQuestions:
        'No recovery questions are set, so the PIN cannot be reset. Reinstall the app to clear it (this erases all boards).',
      answerToReset: 'Answer to reset PIN',
      answerHint: 'Answer every question correctly (not case-sensitive).',
      answerPlaceholder: 'Answer',
      verify: 'Verify',
      answersIncorrect: 'One or more answers are incorrect.',
      setNewPin: 'Set a new PIN',
      newPinPlaceholder: 'New PIN (min 4 digits)',
      confirmNewPinPlaceholder: 'Confirm new PIN',
      saveNewPin: 'Save new PIN',
      pinTooShort: 'PIN must be at least 4 digits.',
      pinsDoNotMatch: 'PINs do not match.',
    },

    colorPicker: {
      title: 'Pick a color',
      hue: 'Hue: {deg}°',
      ok: 'OK',
    },

    editItem: {
      name: 'Name',
      itemLanguage: 'Item language',
      language: 'Language',
      labelsByLanguage: 'Labels by language',
      add: 'Add',
      addLanguage: '+ Add language',
      image: 'Image',
      loading: 'Loading…',
      loadMoreImages: 'Load more images',
      findImages: 'Find images',
      chooseCustomImage: 'Choose custom image',
      resetToDefault: 'Reset to default',
      audio: 'Audio',
      useItemName: 'Use item name',
      useCustomAudio: 'Use custom audio',
      audioClip: 'Audio clip',
      record: 'Record',
      stop: 'Stop',
      importFromDevice: 'Import from device',
      preview: '▶ Preview',
      clearAudio: 'Clear audio',
      renameAudioClip: 'Rename audio clip',
      clipNamePlaceholder: 'Clip name',
      pronHint: 'Optional: respell the name so it\'s spoken correctly (e.g. "MIS-chiv-us").',
      pronPlaceholder: 'Pronunciation',
      color: 'Color',
      custom: 'Custom',
      default: 'Default',
      editCustomColor: 'Edit custom color',
      pickCustomColor: 'Pick custom color',
      ttsBehavior: 'TTS behavior',
      delete: 'Delete',
    },
  },
};

// The shape of the full string table. Translated language tables conform to this type.
export type Strings = typeof STRINGS;

// Interpolate {token} placeholders in a template string, e.g. fmt('Hi {name}', { name: 'A' }).
// Unknown tokens are left untouched so a missing var is visible rather than silently blank.
export function fmt(template: string, vars: Record<string, string | number>): string {
  return template.replace(/\{(\w+)\}/g, (_, k) => (k in vars ? String(vars[k]) : `{${k}}`));
}

// English default. Components read the active-language table via useStrings() (see i18n.ts).
export const S = STRINGS;
