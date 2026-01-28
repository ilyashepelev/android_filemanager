package ru.shepelev.filemanager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private ArrayList<File> fileList = new ArrayList<>();
    private File currentDir;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MANAGE_STORAGE_REQUEST_CODE = 101;


    private static final String TAG = "FileManager";

    ImageButton btnRequestPermission, btnReplay, btnSort;


    // В начало класса добавьте:
    private static final int SORT_NAME_ASC = 0;
    private static final int SORT_NAME_DESC = 1;
    private static final int SORT_SIZE_ASC = 2;
    private static final int SORT_SIZE_DESC = 3;
    private static final int SORT_DATE_ASC = 4;
    private static final int SORT_DATE_DESC = 5;
    private int currentSortType = SORT_NAME_ASC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        fileAdapter = new FileAdapter(fileList, new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(File file) {
                handleFileClick(file);
            }

            @Override
            public void onItemLongClick(File file, int position) {
                showFileOptionsDialog(file, position);
            }
        });



        btnRequestPermission = findViewById(R.id.btnRequestPermission);
        btnRequestPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermissions();
            }
        });

        checkAndRequestPermissions();

        btnReplay = findViewById(R.id.btnReplay);
        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFiles();
            }
        });


        btnSort = findViewById(R.id.btnSort);
        btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortDialog();
            }
        });

    }
    private void showSortDialog() {
        String[] sortOptions = {
                "По имени (А-Я)",
                "По имени (Я-А)",
                "По размеру (возр.)",
                "По размеру (убыв.)",
                "По дате (старые)",
                "По дате (новые)"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Сортировка")
                .setSingleChoiceItems(sortOptions, currentSortType, (dialog, which) -> {
                    currentSortType = which;
                    sortFiles(currentSortType);
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    private void sortFiles(int sortType) {
        if (fileList == null || fileList.size() == 0) return;

        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                // Всегда оставляем ".." наверху
                if (f1.getName().equals("..")) return -1;
                if (f2.getName().equals("..")) return 1;

                boolean f1IsDir = f1.isDirectory() || f1.getPath().contains("|");
                boolean f2IsDir = f2.isDirectory() || f2.getPath().contains("|");

                // Если разные типы (папка/файл)
                if (f1IsDir && !f2IsDir) return -1;
                if (!f1IsDir && f2IsDir) return 1;

                // Если одинаковые типы - сортируем по выбранному критерию
                String realPath1 = f1.getPath().contains("|") ?
                        f1.getPath().split("\\|")[0] : f1.getPath();
                String realPath2 = f2.getPath().contains("|") ?
                        f2.getPath().split("\\|")[0] : f2.getPath();

                File realFile1 = new File(realPath1);
                File realFile2 = new File(realPath2);

                switch (sortType) {

                    case SORT_NAME_ASC:
                        return f1.getName().toLowerCase()
                                .compareTo(f2.getName().toLowerCase());

                    case SORT_NAME_DESC:
                        return f2.getName().toLowerCase()
                                .compareTo(f1.getName().toLowerCase());

                    case SORT_SIZE_ASC:
                        return Long.compare(realFile1.length(), realFile2.length());

                    case SORT_SIZE_DESC:
                        return Long.compare(realFile2.length(), realFile1.length());

                    case SORT_DATE_ASC:
                        return Long.compare(realFile1.lastModified(), realFile2.lastModified());

                    case SORT_DATE_DESC:
                        return Long.compare(realFile2.lastModified(), realFile1.lastModified());

                    default:
                        return f1.getName().toLowerCase()
                                .compareTo(f2.getName().toLowerCase());
                }
            }
        });

        fileAdapter.notifyDataSetChanged();
    }















    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                if (currentDir == null) {
                    loadFiles();
                }
            }
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 и выше
            if (Environment.isExternalStorageManager()) {
                btnRequestPermission.setVisibility(View.GONE);
                loadFiles();

            } else {
                requestManageAllFilesPermission();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                btnRequestPermission.setVisibility(View.GONE);
                loadFiles();

            } else {
                requestLegacyPermissions();
            }
        } else {
            // Android до 6.0
            btnRequestPermission.setVisibility(View.GONE);
            loadFiles();
        }
    }

    private void requestManageAllFilesPermission() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Требуется доступ к файлам")
                .setMessage("Для полного доступа к файлам необходимо разрешение.\n\n" +
                        "После нажатия 'Разрешить':\n" +
                        "1. Найдите это приложение в настройках\n" +
                        "2. Включите 'Разрешить управление всеми файлами'\n" +
                        "3. Вернитесь в приложение")
                .setPositiveButton("Разрешить", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                    } catch (Exception e) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                    }
                })
                .setNegativeButton("Публичные папки", (dialog, which) -> {
                    loadFilesFromPublicDirectories();
                })
                .setCancelable(false)
                .show();
    }

    private void requestLegacyPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Доступ разрешен!", Toast.LENGTH_SHORT).show();
                    btnRequestPermission.setVisibility(View.GONE);
                    loadFiles();
                } else {
                    Toast.makeText(this, "Используем публичные папки", Toast.LENGTH_LONG).show();
                    loadFilesFromPublicDirectories();
                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                loadFiles();
            } else {
                Toast.makeText(this,
                        "Используем публичные папки",
                        Toast.LENGTH_LONG).show();
                loadFilesFromPublicDirectories();
            }
        }
    }

    private void loadFiles() {
        try {
            File root = Environment.getExternalStorageDirectory();
            if (root != null && root.exists()) {
                loadDirectory(root);
            } else {
                loadFilesFromPublicDirectories();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            loadFilesFromPublicDirectories();
        }
    }

    private void showFileOptionsDialog(File file, int position) {
        String realPath = file.getPath();
        if (realPath.contains("|")) {
            realPath = realPath.split("\\|")[0];
        }
        File realFile = new File(realPath);

        ArrayList<String> options = new ArrayList<>();
        options.add("Открыть");
        options.add("Поделиться");
        options.add("Свойства");

        if (!file.getName().equals("..")) {
            options.add("Переименовать");
            options.add("Удалить");
        }

        final CharSequence[] items = options.toArray(new CharSequence[0]);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(items, (dialog, which) -> {
                    String selectedOption = options.get(which);
                    switch (selectedOption) {
                        case "Открыть":
                            if (file.getName().equals("..")) {
                                handleBackNavigation();
                            } else if (file.isDirectory() || file.getPath().contains("|")) {
                                handleFileClick(file);
                            } else {
                                openFileWithSystem(realFile);
                            }
                            break;

                        case "Поделиться":
                            if (!file.getName().equals("..")) {
                                shareFile(realFile);
                            }
                            break;

                        case "Свойства":
                            showFileInfo(realFile);
                            break;
                        case "Переименовать":
                            renameFile(realFile);
                            break;
                        case "Удалить":
                            deleteFileWithConfirmation(realFile, position);
                            break;

                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteFileWithConfirmation(File file, int position) {
        String message;
        if (file.isDirectory()) {
            int fileCount = countFilesInDirectory(file);
            message = String.format(Locale.getDefault(),
                    "Удалить папку \"%s\"?\nВ папке: %d файлов\n\nУдаление невозможно отменить!",
                    file.getName(), fileCount);
        } else {
            message = String.format("Удалить файл \"%s\"?\nРазмер: %s\n\nУдаление невозможно отменить!",
                    file.getName(), formatFileSize(file.length()));
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Подтверждение удаления")
                .setMessage(message)
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteFile(file, position);
                })
                .setNegativeButton("Отмена", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private int countFilesInDirectory(File directory) {
        int count = 0;
        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        count += countFilesInDirectory(file);
                    }
                    count++;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting files: " + e.getMessage());
        }
        return count;
    }

    private void deleteFile(File file, int position) {
        try {
            boolean deleted = false;

            if (file.isDirectory()) {
                deleted = deleteDirectory(file);
            } else {
                deleted = file.delete();
            }

            if (deleted) {
                // Удаляем из списка
                if (position >= 0 && position < fileList.size()) {
                    fileList.remove(position);
                    fileAdapter.notifyItemRemoved(position);
                }

                Toast.makeText(this,
                        file.isDirectory() ? "Папка удалена" : "Файл удален",
                        Toast.LENGTH_SHORT).show();

                // Обновляем список
                if (currentDir != null) {
                    loadDirectory(currentDir);
                }
            } else {
                Toast.makeText(this, "Не удалось удалить", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Нет прав на удаление: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Delete permission error: " + e.getMessage());
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при удалении: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Delete error: " + e.getMessage());
        }
    }

    private boolean deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }

        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            Log.w(TAG, "Failed to delete file: " + file.getPath());
                        }
                    }
                }
            }
            return directory.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting directory: " + e.getMessage());
            return false;
        }
    }

    private void renameFile(final File file) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Переименовать");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(file.getName());
        input.setSelectAllOnFocus(true);

        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(file.getName())) {
                performRename(file, newName);
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();


        input.postDelayed(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
    }

    private void performRename(File file, String newName) {
        try {
            File newFile = new File(file.getParent(), newName);

            if (newFile.exists()) {
                Toast.makeText(this, "Файл с таким именем уже существует", Toast.LENGTH_SHORT).show();
                return;
            }

            if (file.renameTo(newFile)) {
                Toast.makeText(this, "Переименовано", Toast.LENGTH_SHORT).show();
                if (currentDir != null) {
                    loadDirectory(currentDir);
                }
            } else {
                Toast.makeText(this, "Не удалось переименовать", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    // Метод для отправки файла
    private void shareFile(File file) {
        try {
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        file
                );
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(getMimeType(file.getName()));
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Поделиться файлом"));
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при отправке: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void loadFilesFromPublicDirectories() {
        fileList.clear();
        currentDir = null;

        // Добавляем публичные папки с понятными именами
        addPublicDirectory("Загрузки", Environment.DIRECTORY_DOWNLOADS);
        addPublicDirectory("Документы", Environment.DIRECTORY_DOCUMENTS);
        addPublicDirectory("Изображения", Environment.DIRECTORY_PICTURES);
        addPublicDirectory("Музыка", Environment.DIRECTORY_MUSIC);
        addPublicDirectory("Видео", Environment.DIRECTORY_MOVIES);
        addPublicDirectory("Камера", Environment.DIRECTORY_DCIM);

        // Внутреннее хранилище приложения
        File internalDir = getFilesDir();
        if (internalDir != null && internalDir.exists()) {
            File wrapper = new File(internalDir.getAbsolutePath()) {
                @Override
                public String getName() {
                    return "Внутреннее хранилище";
                }

                @Override
                public boolean isDirectory() {
                    return true;
                }
            };
            fileList.add(wrapper);
        }

        updateAdapter();
        setTitle("Публичные папки");
    }

    private void addPublicDirectory(String displayName, String directoryType) {
        File dir = Environment.getExternalStoragePublicDirectory(directoryType);
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File wrapper = new File(dir.getAbsolutePath()) {
                @Override
                public String getName() {
                    return displayName;
                }

                @Override
                public boolean isDirectory() {
                    return true;
                }

                @Override
                public String getPath() {
                    return super.getPath() + "|" + displayName;
                }
            };
            fileList.add(wrapper);
        }
    }

    private void loadDirectory(File dir) {
        currentDir = dir;
        fileList.clear();

        try {
            File[] files = dir.listFiles();
            if (files != null) {
                // Добавляем родительскую директорию
                if (!dir.equals(Environment.getExternalStorageDirectory()) && dir.getParentFile() != null) {
                    fileList.add(new File(".."));
                }

                // Добавляем файлы и папки
                for (File file : files) {
                    if (!file.isHidden()) {
                        fileList.add(file);
                    }
                }

                // Сортируем
                sortFiles(currentSortType);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            Toast.makeText(this, "Нет доступа к папке", Toast.LENGTH_SHORT).show();
            return;
        }

        updateAdapter();
        setTitle(dir.getName());
    }

    private void updateAdapter() {
        fileAdapter = new FileAdapter(fileList, new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(File file) {
                handleFileClick(file);
            }

            @Override
            public void onItemLongClick(File file, int position) {
                showFileOptionsDialog(file, position);
            }

        });

        recyclerView.setAdapter(fileAdapter);
    }

    private void handleFileClick(File file) {
        if (file.getName().equals("..")) {
            handleBackNavigation();
        } else if (file.getPath().contains("|")) {
            // Это публичная папка из нашего списка
            String realPath = file.getPath().split("\\|")[0];
            File realDir = new File(realPath);
            if (realDir.exists() && realDir.isDirectory()) {
                loadDirectory(realDir);
            }
        } else if (file.isDirectory()) {
            loadDirectory(file);
        } else {
            // Открываем файл
            openFileWithSystem(file);
        }
    }

    private void handleBackNavigation() {
        if (currentDir == null) {
            loadFilesFromPublicDirectories();
        } else {
            File parent = currentDir.getParentFile();
            if (parent != null && parent.exists()) {
                loadDirectory(parent);
            } else {
                loadFiles();
            }
        }
    }

    private void openFileWithSystem(File file) {
        try {
            // Проверяем существует ли файл
            if (!file.exists()) {
                Toast.makeText(this, "Файл не найден: " + file.getName(), Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверяем можем ли мы читать файл
            if (!file.canRead()) {
                Toast.makeText(this, "Нет прав для чтения файла", Toast.LENGTH_SHORT).show();
                return;
            }

            // Получаем MIME тип
            String mimeType = getMimeType(file.getName());

            // Создаем Intent для открытия файла
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // Получаем URI файла
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Для Android 7+ используем FileProvider
                try {
                    fileUri = FileProvider.getUriForFile(
                            MainActivity.this,
                            getPackageName() + ".provider",
                            file
                    );
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "FileProvider error: " + e.getMessage());
                    // Пробуем обычный способ
                    fileUri = Uri.fromFile(file);
                }
            } else {
                fileUri = Uri.fromFile(file);
            }

            // Устанавливаем данные и тип
            intent.setDataAndType(fileUri, mimeType);

            // Добавляем флаг для чтения
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Проверяем есть ли приложение для открытия
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Открыть с помощью:"));
            } else {
                // Пробуем общий тип
                intent.setDataAndType(fileUri, "*/*");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(intent, "Открыть с помощью:"));
                } else {
                    Toast.makeText(this,
                            "Нет приложения для открытия файла " + file.getName(),
                            Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening file: " + e.getMessage(), e);
            Toast.makeText(this,
                    "Ошибка при открытии файла: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String getMimeType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }

        switch (extension) {
            case "txt": return "text/plain";
            case "pdf": return "application/pdf";
            case "jpg": case "jpeg": case "JPG": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "ogg": return "audio/ogg";
            case "mp4": return "video/mp4";
            case "avi": return "video/x-msvideo";
            case "mkv": return "video/x-matroska";
            case "doc": case "docx": return "application/msword";
            case "xls": case "xlsx": return "application/vnd.ms-excel";
            case "ppt": case "pptx": return "application/vnd.ms-powerpoint";
            case "zip": return "application/zip";
            case "rar": return "application/x-rar-compressed";
            case "html": case "htm": return "text/html";
            case "xml": return "text/xml";
            case "json": return "application/json";
            default: return "*/*";
        }
    }

    private void showFileInfo(File file) {
        String realPath = file.getPath();
        String displayName = file.getName();

        if (realPath.contains("|")) {
            String[] parts = realPath.split("\\|");
            realPath = parts[0];
            displayName = parts[1];
        }

        File realFile = new File(realPath);
        String info = "Имя: " + displayName + "\n" +
                "Путь: " + realPath + "\n";

        if (realFile.exists()) {
            info += "Размер: " + formatFileSize(realFile.length()) + "\n" +
                    "Тип: " + (realFile.isDirectory() ? "Папка" : "Файл") + "\n" +
                    "Изменен: " + new java.util.Date(realFile.lastModified());
        } else {
            info += "Файл не существует";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Информация о файле")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 Б";
        final String[] units = new String[]{"Б", "КБ", "МБ", "ГБ", "ТБ"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
