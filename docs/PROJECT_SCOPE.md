# Blink Project Scope

## Supported Features (V1)

Blink V1 focuses purely on high-fidelity, high-speed document viewing.

### 1. Document Integration & File System Handling
* **System File Handler**: Registers as a handler for supported MIME types, allowing users to select Blink to open documents from other apps (WhatsApp, Gmail, Telegram, Files, etc.).
* **URI Handling**: Support for opening files from `content://` and `file://` URIs.

### 2. Format Support
* **PDF (.pdf)**: Full page rendering, continuous scroll, pinch-to-zoom.
* **Word Documents (.doc, .docx)**: Text extraction, paragraph styling, basic layout rendering.
* **Excel Spreadsheets (.xls, .xlsx)**: Sheet-based navigation, grid rendering, column/row alignment.
* **PowerPoint Presentations (.ppt, .pptx)**: Slide-by-slide viewing, scaled slide rendering.
* **Text files (.txt)**: Quick text layout rendering with word wrapping, font size scaling.
* **CSV Spreadsheets (.csv)**: Comma/semicolon delimited file parser, grid sheet display.

### 3. Viewer Interaction
* **Pinch-to-Zoom**: Smooth zooming gestures across all document formats.
* **Search Within Document**: Basic local text search (find on page) within the currently viewed file.
* **Dark Mode Support**: Dynamic system-matching dark mode layout inversion or text coloring adjustment for comfortable reading in low light.

---

## Unsupported Features (V1)

To maintain extreme performance and speed, the following features are not supported in V1:
* **Editing**: Any form of modification, saving, or exporting changed documents.
* **Annotations**: Drawing, highlighting, adding notes, signatures, or forms filling.
* **Document Conversion**: Converting between formats (e.g., DOCX to PDF).
* **Metadata Modification**: Renaming files, editing author tags, or updating metadata.

---

## Future Possibilities (V2+)

The following features may be considered for future iterations, provided they do not compromise the Cold Start target of `< 500ms`:
* **Recent File History**: A lightweight local home dashboard showing recently opened documents (stored via Proto DataStore).
* **Bookmarks**: Local bookmarks to quickly return to a specific page or section in a file.
* **Print Support**: Native integration with the Android Print Framework to print viewed documents.

---

## Explicit Exclusions

The following features will **never** be built into Blink, as they violate its core identity:
* **Cloud Integrations**: Firebase, Supabase, Google Drive sync, Dropbox, or any other cloud-based file repository.
* **User Accounts**: Login, signup, authentication, profiles, or subscription states.
* **Collaborative Tools**: Direct comments, sharing sessions, or chat features.
* **AI & Processing Engine**: OCR, AI summary engines, translation tools, or text-to-speech.
* **Telemetry & Ads**: Analytics platforms, telemetry, crash reporting libraries (unless self-hosted and offline-safe), or third-party ads networks.
