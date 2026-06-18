public class Complaint {

    private static int counter = 0;

    private int id;
    private String user;
    private String text;
    private String category;
    private String status;
    private String adminNote;

    // Standard Constructor for NEW complaints (Auto-increments ID)
    public Complaint(String user, String text, String category) {
        this.id = ++counter;
        this.user = user;
        this.text = text;
        this.category = category;
        this.status = "Pending";
        this.adminNote = ""; 
    }

    // Overloaded Constructor for LOADING existing complaints from file
    public Complaint(int id, String user, String category, String text, String status, String adminNote) {
        this.id = id;
        this.user = user;
        this.category = category;
        this.text = text;
        this.status = status;
        this.adminNote = (adminNote == null) ? "" : adminNote;
        
        // Keep the global counter in sync with the highest ID loaded
        counter = Math.max(counter, id);
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getUser() { return user; }
    public String getText() { return text; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String note) {
        this.adminNote = note;
    }
}