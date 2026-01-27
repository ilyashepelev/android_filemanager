package ru.shepelev.filemanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    private ArrayList<File> fileList;
    private OnItemClickListener listener;
    private int selectedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(File file);
        void onItemLongClick(File file, int position);
    }

    public FileAdapter(ArrayList<File> fileList, OnItemClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.bind(file, position, listener);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView nameTextView;
        private TextView infoTextView;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            infoTextView = itemView.findViewById(R.id.infoTextView);
        }

        public void bind(final File file, final int position, final OnItemClickListener listener) {
            // Установка иконки и текста
            if (file.getName().equals("..")) {
                iconImageView.setImageResource(R.drawable.outline_arrow_back_24);
                nameTextView.setText("...");
                infoTextView.setText("Назад");
            } else if (file.isDirectory() || file.getPath().contains("|")) {
                iconImageView.setImageResource(R.drawable.twotone_folder_24);
                nameTextView.setText(file.getName());
                infoTextView.setText("Папка");
            } else {
                iconImageView.setImageResource(getFileIcon(file.getName()));
                nameTextView.setText(file.getName());

                String realPath = file.getPath();
                if (realPath.contains("|")) {
                    realPath = realPath.split("\\|")[0];
                }
                File realFile = new File(realPath);
                infoTextView.setText(formatFileInfo(realFile));
            }

            // Обработка клика
            itemView.setOnClickListener(v -> listener.onItemClick(file));

            // Обработка долгого нажатия
            itemView.setOnLongClickListener(v -> {
                selectedPosition = position;
                listener.onItemLongClick(file, position);
                return true;
            });
        }

        private int getFileIcon(String fileName) {
            if (fileName.endsWith(".txt")) {
                return R.drawable.twotone_text_fields_24;
            } else if (fileName.endsWith(".pdf")) {
                return R.drawable.twotone_picture_as_pdf_24;
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                    || fileName.endsWith(".png") || fileName.endsWith(".gif")) {
                return R.drawable.twotone_image_24;
            } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")
                    || fileName.endsWith(".ogg")) {
                return R.drawable.twotone_audio_file_24;
            } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi")
                    || fileName.endsWith(".mkv")) {
                return R.drawable.twotone_video_file_24;
            } else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
                return R.drawable.twotone_archive_24;
            } else {
                return R.drawable.twotone_insert_drive_file_24;
            }
        }

        private String formatFileInfo(File file) {
            if (file.exists()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                String date = sdf.format(new Date(file.lastModified()));
                String size = formatFileSize(file.length());
                return date + " | " + size;
            }
            return "Файл";
        }

        private String formatFileSize(long size) {
            if (size <= 0) return "0 Б";
            final String[] units = new String[]{"Б", "КБ", "МБ", "ГБ", "ТБ"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
        }
    }

}
