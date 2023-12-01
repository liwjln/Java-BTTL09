import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileCopyWithProgress {
    public static void main(String[] args) {
        // Create a frame
        JFrame frame = new JFrame("File Copy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLocationRelativeTo(null);

        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS)); // Set layout to BoxLayout
        frame.getContentPane().add(buttonPanel, BorderLayout.NORTH);

        // Create a file chooser
        JFileChooser fileChooser = new JFileChooser();

        // Create a button for browsing the source file
        JButton sourceButton = new JButton("Browse Source");
        sourceButton.addActionListener(e -> {
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                sourceButton.setText(fileChooser.getSelectedFile().getPath());
            }
        });
        buttonPanel.add(sourceButton);

        // Create a button for browsing the destination file
        JButton destButton = new JButton("Browse Destination");
        destButton.addActionListener(e -> {
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                destButton.setText(fileChooser.getSelectedFile().getPath());
            }
        });
        buttonPanel.add(destButton);

        // Create a button for starting the copy operation
        JButton copyButton = new JButton("Start Copy");
        buttonPanel.add(copyButton);

        // Create a panel for the progress bars
        JPanel progressBarPanel = new JPanel();
        progressBarPanel.setLayout(new BoxLayout(progressBarPanel, BoxLayout.Y_AXIS)); // Set layout to BoxLayout
        frame.getContentPane().add(progressBarPanel, BorderLayout.CENTER);

        // Create a button for restarting the copy operation
        JButton restartButton = new JButton("Restart Copy");
        restartButton.setVisible(false); // Initially, the button is not visible
        frame.getContentPane().add(restartButton, BorderLayout.SOUTH); // Add the button to the bottom of the frame

        // Display the frame
        frame.setVisible(true);

        restartButton.addActionListener(e -> {
            // Remove all progress bars from the previous operation
            progressBarPanel.removeAll();
            frame.validate();

            // Reset the text of the source and destination buttons
            sourceButton.setText("Browse Source");
            destButton.setText("Browse Destination");

            // Hide the restart button
            restartButton.setVisible(false);
        });

        copyButton.addActionListener(e -> {
            // Define the source and destination files
            File sourceFile = new File(sourceButton.getText());
            File destFile = new File(destButton.getText());

            // Determine the number of threads and the size of each chunk
            int numThreads = 4;
            long chunkSize = sourceFile.length() / numThreads;

            // Create a counter for the completed threads
            AtomicInteger completedThreads = new AtomicInteger(0);

            for (int i = 0; i < numThreads; i++) {
                // Create a progress bar for this thread
                JProgressBar progressBar = new JProgressBar();
                progressBar.setStringPainted(true);
                progressBarPanel.add(progressBar);
                frame.validate();

                // Determine the start and end positions for this chunk
                long startPos = i * chunkSize;
                long endPos = (i == numThreads - 1) ? sourceFile.length() : startPos + chunkSize;

                // Create a thread for the copy operation
                Thread copyThread = new Thread(() -> {
                    try (RandomAccessFile sourceRaf = new RandomAccessFile(sourceFile, "r");
                            RandomAccessFile destRaf = new RandomAccessFile(destFile, "rw")) {

                        // Move the file pointers to the start position
                        sourceRaf.seek(startPos);
                        destRaf.seek(startPos);

                        byte[] buffer = new byte[2048];
                        long bytesTransferred = 0;
                        while (bytesTransferred < endPos - startPos) {
                            int length = sourceRaf.read(buffer);
                            destRaf.write(buffer, 0, length);
                            bytesTransferred += length;

                            // Calculate the progress as a percentage and update the progress bar
                            int progress = (int) (100 * bytesTransferred / (endPos - startPos));
                            SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
                        }

                        // If all threads have completed, show the restart button
                        if (completedThreads.incrementAndGet() == numThreads) {
                            SwingUtilities.invokeLater(() -> restartButton.setVisible(true));
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

                // Start the copy thread
                copyThread.start();
            }
        });
    }
}
