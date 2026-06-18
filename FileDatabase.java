import java.io.*;
import java.util.*;

public class FileDatabase {

    private static final String FILE_NAME = "complaints.txt";

    // Overwrite and save everything back to complaints.txt
    public static void saveAll(List<Complaint> list) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME, false))) {
            for (Complaint c : list) {
                bw.write(c.getId() + "|" +
                         c.getUser() + "|" +
                         c.getCategory() + "|" +
                         c.getText() + "|" +
                         c.getStatus() + "|" +
                         c.getAdminNote());
                bw.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load and rebuild complaints from complaints.txt
    public static List<Complaint> loadAll() {
        List<Complaint> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {

                // Using -1 as a limit parameter ensures trailing empty strings/notes aren't discarded
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;

                // Safely parse the ID out from string parts[0]
                int id = Integer.parseInt(parts[0]);
                String user = parts[1];
                String category = parts[2];
                String text = parts[3];
                String status = parts[4];
                
                // Read the admin note if it exists in parts[5], else keep it empty
                String adminNote = (parts.length >= 6) ? parts[5] : "";

                // Use the new overloaded loading constructor
                Complaint c = new Complaint(id, user, category, text, status, adminNote);

                list.add(c);
            }

        } catch (FileNotFoundException e) {
            System.out.println("No existing database file found. A new one will be created upon submission.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}