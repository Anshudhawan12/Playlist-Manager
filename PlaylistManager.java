// PlaylistManager.java
// This program implements a simple playlist management system with user authentication.
// Users can create playlists, add/remove/update songs, and admins can manage users and view all playlists.

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner; // Importing Scanner class for user input

public class PlaylistManager {
    // class-level variables - designed for shared access across the class.
    static Scanner sc = new Scanner(System.in); // ensures all methods use the same Scanner instance.
    // This means the variable belongs to the class, not an instance. So any method (even static main()) can use it without creating an object of the class.
    // It’s used here because main is a static method and static methods can only access static variables.
    static final String USER_FILE = "users.txt"; // final to prevent accidental modification after initialization
    // Because "users.txt" is a fixed file path used for saving/loading users. Marking it final prevents accidental changes.

    // Static Inner class representing a user in the system
    static class User {
        String username, password, role;
        User next; // For linked list of users

        User(String u, String p, String r) {
            username = u;
            password = p;
            role = r;
        }
    }

    // Static Inner class representing a Song in a playlist
    static class Song {
        String title, artist;
        Song next; // For linked list of songs

        Song(String t, String a) {
            title = t;
            artist = a;
        }

        // Converts song to string for file storage
        public String toString() {
            return title + "," + artist;
        }

        // Parses a song from a string (used when loading from file)
        static Song fromString(String line) {
            String[] parts = line.split(",");
            return new Song(parts[0], parts[1]);
        }
    }

    // Static Inner class representing a Playlist (linked list of songs)
    static class Playlist {
        String name; // Playlist name
        Song head;   // Head of song linked list
        Playlist next; // For linked list of playlists

        Playlist(String n) {
            name = n;
        }

        // Adds a song to the end of the playlist
        void addSong(String title, String artist) {
            Song s = new Song(title, artist);
            if (head == null) head = s;
            else {
                Song temp = head;
                while (temp.next != null) temp = temp.next;
                temp.next = s;
            }
        }

        // Displays all songs in the playlist
        void displaySongs() {
            if (head == null) System.out.println("No songs.");
            else {
                int i = 1;
                Song temp = head;
                while (temp != null) {
                    System.out.println(i++ + ". " + temp.title + " by " + temp.artist);
                    temp = temp.next;
                }
            }
        }

        // Returns the song at the given index (1-based)
        Song getSong(int index) {
            int i = 1;
            Song temp = head;
            while (temp != null) {
                if (i == index) return temp;
                i++;
                temp = temp.next;
            }
            return null;
        }

        // Removes the song at the given index (1-based)
        boolean removeSong(int index) {
            if (index == 1) {
                if (head != null) {
                    head = head.next;
                    return true;
                }
            }
            int i = 1;
            Song prev = null;
            Song curr = head;
            while (curr != null) {
                if (i == index) {
                    if (prev != null) prev.next = curr.next;
                    return true;
                }
                prev = curr;
                curr = curr.next;
                i++;
            }
            return false;
        }

        // Saves the playlist to a file (one song per line)
        void saveToFile(String user) throws IOException {
            // Difference b/w FileWriter and BufferedWriter:
            // FileWriter writes data to a file directly, while BufferedWriter uses a buffer to improve performance by reducing the number of write operations.
            BufferedWriter bw = new BufferedWriter(new FileWriter("playlist-" + user + "-" + name + ".txt"));
            Song temp = head;
            while (temp != null) {
                bw.write(temp.toString());
                bw.newLine();
                temp = temp.next;
            }
            bw.close();
        }

        // Loads the playlist from a file (replaces current songs)
        void loadFromFile(String user) throws IOException {
            head = null;
            File f = new File("playlist-" + user + "-" + name + ".txt");
            if (!f.exists()) return;

            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                Song s = Song.fromString(line);
                addSong(s.title, s.artist);
            }
            br.close();
        }
    }

    // Linked list of Playlists for a user
    static class PlaylistList {
        Playlist head;

        // Adds a new playlist to the end of the list
        void add(String name) {
            Playlist p = new Playlist(name);
            if (head == null) head = p;
            else {
                Playlist temp = head;
                while (temp.next != null) temp = temp.next;
                temp.next = p;
            }
        }

        // Returns the playlist at the given index (1-based)
        Playlist get(int index) {
            int i = 1;
            Playlist temp = head;
            while (temp != null) {
                if (i == index) return temp;
                i++;
                temp = temp.next;
            }
            return null;
        }

        // Displays all playlists for the user (with song count)
        void display(String user) {
            sort(); // Sort playlists before displaying
            int i = 1;
            Playlist temp = head;
            while (temp != null) {
                try {
                    temp.loadFromFile(user);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int songCount = 0;
                Song s = temp.head;
                while (s != null) {
                    songCount++;
                    s = s.next;
                }
                System.out.println(i++ + ". " + temp.name + " (Songs: " + songCount + ")");
                temp = temp.next;
            }
        }

        // Loads all playlists for a user from files
        void loadAll(String user) throws IOException {
            File[] files = new File(".").listFiles((dir, name) -> name.startsWith("playlist-" + user + "-") && name.endsWith(".txt"));
            if (files == null) return;
            for (File f : files) {
                String name = f.getName().replace("playlist-" + user + "-", "").replace(".txt", "");
                Playlist p = new Playlist(name);
                p.loadFromFile(user);
                if (head == null) head = p;
                else {
                    Playlist temp = head;
                    while (temp.next != null) temp = temp.next;
                    temp.next = p;
                }
            }
        }

        // Sorts playlists alphabetically by name (case-insensitive)
        void sort() {
            if (head == null || head.next == null) {
                return; // Empty or single-node list is already sorted
            }

            boolean swapped;
            do {
                swapped = false;
                Playlist current = head;
                Playlist prev = null;

                while (current != null && current.next != null) {
                    Playlist next = current.next;
                    // Compare playlist names case-insensitively
                    if (current.name.compareToIgnoreCase(next.name) > 0) {
                        // Swap nodes
                        swapped = true;
                        if (prev == null) {
                            // Swapping at the head
                            head = next;
                            current.next = next.next;
                            next.next = current;
                        } else {
                            // Swapping in the middle or end
                            prev.next = next;
                            current.next = next.next;
                            next.next = current;
                        }
                        // Update prev to point to the new position of current
                        prev = next;
                    } else {
                        // No swap, move to next pair
                        prev = current;
                        current = next;
                    }
                }
            } while (swapped); // Continue until no swaps are needed
        }
    }

    // Linked list of Users
    static class UserList {
        User head;

        // Loads users from file into linked list
        void load() throws IOException {
            head = null;
            File f = new File(USER_FILE);
            if (!f.exists()) f.createNewFile();

            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                add(data[0], data[1], data[2]);
            }
            br.close();
        }

        // Saves all users to file
        void save() throws IOException {
            BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE));
            User temp = head;
            while (temp != null) {
                bw.write(temp.username + "," + temp.password + "," + temp.role);
                bw.newLine();
                temp = temp.next;
            }
            bw.close();
        }

        // Adds a new user to the end of the list
        void add(String u, String p, String r) {
            User user = new User(u, p, r);
            if (head == null) head = user;
            else {
                User temp = head;
                while (temp.next != null) temp = temp.next;
                temp.next = user;
            }
        }

        // Finds a user by username and password
        User find(String u, String p) {
            User temp = head;
            while (temp != null) {
                if (temp.username.equals(u) && temp.password.equals(p)) return temp;
                temp = temp.next;
            }
            return null;
        }

        // Checks if a username already exists
        boolean exists(String u) {
            User temp = head;
            while (temp != null) {
                if (temp.username.equals(u)) return true;
                temp = temp.next;
            }
            return false;
        }

        // Displays all users
        void display() {
            int i = 1;
            User temp = head;
            while (temp != null) {
                System.out.println(i++ + ". " + temp.username + " (" + temp.role + ")");
                temp = temp.next;
            }
        }

        // Removes a user at the given index (1-based)
        boolean remove(int index) {
            if (index == 1) {
                if (head != null) {
                    head = head.next;
                    return true;
                }
            }
            int i = 1;
            User prev = null;
            User curr = head;
            while (curr != null) {
                if (i == index) {
                    if (prev != null) prev.next = curr.next;
                    return true;
                }
                prev = curr;
                curr = curr.next;
                i++;
            }
            return false;
        }

        // Returns the user at the given index (1-based)
        User get(int index) {
            int i = 1;
            User temp = head;
            while (temp != null) {
                if (i == index) return temp;
                temp = temp.next;
                i++;
            }
            return null;
        }
    }

    // Helper method to get integer input from user with prompt
    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // Allows user to update (rename) a song in a playlist
    static void updatePlaylist(String username, PlaylistList playlists) throws IOException {
        if (playlists.head == null) {
            System.out.println("No playlists available.");
            return;
        }

        System.out.println("Your playlists:");
        playlists.display(username);
        System.out.print("Choose a playlist to update (1-N): ");
        int playlistIndex = Integer.parseInt(sc.nextLine());
        Playlist selectedPlaylist = playlists.get(playlistIndex);

        if (selectedPlaylist == null) {
            System.out.println("Invalid playlist selection.");
            return;
        }

        selectedPlaylist.loadFromFile(username);

        if (selectedPlaylist.head == null) {
            System.out.println("This playlist is empty.");
            return;
        }

        System.out.println("Songs in " + selectedPlaylist.name + ":");
        selectedPlaylist.displaySongs();

        System.out.print("Choose a song to rename (1-N): ");
        int songIndex = Integer.parseInt(sc.nextLine());
        Song selectedSong = selectedPlaylist.getSong(songIndex);

        if (selectedSong == null) {
            System.out.println("Invalid song selection.");
            return;
        }

        System.out.println("Current song title: " + selectedSong.title);
        System.out.print("Enter new song title: ");
        String newTitle = sc.nextLine();

        System.out.print("Enter new artist name (or press Enter to keep current '" + selectedSong.artist + "'): ");
        String newArtist = sc.nextLine();

        selectedSong.title = newTitle;
        if (!newArtist.trim().isEmpty()) {
            selectedSong.artist = newArtist;
        }

        selectedPlaylist.saveToFile(username);
        System.out.println("Song updated successfully!");
    }

    // Main method: Entry point of the program
    public static void main(String[] args) throws IOException {
        // ANSI color codes for colored output in terminal
        String CYAN = "\u001B[36m";
        String MAGENTA = "\u001B[35m";
        String GREEN = "\u001B[32m";
        String YELLOW = "\u001B[33m";
        String RESET = "\u001B[0m";

        // Welcome banner
        System.out.println(GREEN + "==================================================" + RESET);
        System.out.println(CYAN + "  _____   __  __   _____ " + RESET);
        System.out.println(CYAN + " |  __ \\ |  \\/  | / ____|" + RESET);
        System.out.println(CYAN + " | |__) || \\  / || (___  " + RESET);
        System.out.println(CYAN + " |  ___/ | |\\/| | \\___ \\ " + RESET);
        System.out.println(CYAN + " | |     | |  | | ____) |" + RESET);
        System.out.println(CYAN + " |_|     |_|  |_||_____/ " + RESET);
        System.out.println(CYAN + "                         " + RESET);
        System.out.println(YELLOW + "PLAYLIST MANAGEMENT SYSTEM" + RESET);
        System.out.println(MAGENTA + "~ Manage your playlists and songs with ease!" + RESET);
        System.out.println(GREEN + "==================================================" + RESET);

        // Load users from file, add default admin if not present
        UserList users = new UserList();
        users.load();
        if (!users.exists("admin")) {
            users.add("admin", "admin123", "Admin");
            users.save();
        }

        // Main menu loop
        while (true) {
            System.out.println(CYAN + "\n=== Music Playlist System ===" + RESET);
            System.out.println(MAGENTA + "1. Login" + RESET);
            System.out.println(MAGENTA + "2. Register" + RESET);
            System.out.println(MAGENTA + "3. Register Multiple Users" + RESET);
            System.out.println(MAGENTA + "4. Exit" + RESET);
            System.out.print(YELLOW + "Choose an option: " + RESET);
            int ch = Integer.parseInt(sc.nextLine());

            if (ch == 1) {
                // Login flow
                System.out.print("Enter username: ");
                String user = sc.nextLine();
                System.out.print("Enter password: ");
                String pass = sc.nextLine();
                User u = users.find(user, pass);
                if (u != null) {
                    if (u.role.equals("Admin")) adminMenu(users);
                    else userMenu(user);
                } else System.out.println("Invalid username or password.");
            } else if (ch == 2) {
                // Register new user
                System.out.print("Enter username: ");
                String u = sc.nextLine();
                System.out.print("Enter password: ");
                String p = sc.nextLine();
                if (users.exists(u)) System.out.println("Username already exists.");
                else {
                    users.add(u, p, "User");
                    users.save();
                    System.out.println("User " + u + " registered successfully!");
                }
            } else if (ch == 3) {
                // Register multiple users at once
                registerMultipleUsers(users);
            } else if (ch == 4) {
                // Exit program
                System.out.println(YELLOW+"Thank you for using Music Playlist System. Goodbye!"+RESET);
                break;
            } else System.out.println("Invalid choice.");
        }
    }

    // Allows admin to register multiple users in one go
    static void registerMultipleUsers(UserList users) throws IOException {
        System.out.println("\n=== Register Multiple Users ===");
        System.out.print("Enter number of users to register: ");
        int numUsers = Integer.parseInt(sc.nextLine());
        
        int successCount = 0;
        for (int i = 1; i <= numUsers; i++) {
            System.out.println("\nRegistering User " + i + "/" + numUsers);
            System.out.print("Enter username: ");
            String username = sc.nextLine();
            
            if (users.exists(username)) {
                System.out.println("Username '" + username + "' already exists. Skipping this user.");
                continue;
            }
            
            System.out.print("Enter password: ");
            String password = sc.nextLine();
            
            users.add(username, password, "User");
            successCount++;
            System.out.println("User '" + username + "' registered successfully!");
        }
        
        if (successCount > 0) {
            users.save();
            System.out.println("\nSuccessfully registered " + successCount + " out of " + numUsers + " users.");
        } else {
            System.out.println("\nNo users were registered.");
        }
    }

    // Menu for regular users (playlist management)
    static void userMenu(String username) throws IOException {
        PlaylistList playlists = new PlaylistList();
        playlists.loadAll(username);
        String CYAN = "\u001B[36m";
        String YELLOW = "\u001B[33m";
        String GREEN = "\u001B[32m";
        String MAGENTA = "\u001B[35m";
        String RESET = "\u001B[0m";

        while (true) {
            System.out.println(GREEN + "\n===================================" + RESET);
            System.out.println(CYAN + "=== User Menu ===" + RESET);
            System.out.println(GREEN + "===================================" + RESET);
            System.out.println(MAGENTA + "1. Create a playlist" + RESET);
            System.out.println(MAGENTA + "2. Add songs to a playlist" + RESET);
            System.out.println(MAGENTA + "3. Remove songs from a playlist" + RESET);
            System.out.println(MAGENTA + "4. Play a song from a playlist" + RESET);
            System.out.println(MAGENTA + "5. Update an existing playlist" + RESET);
            System.out.println(MAGENTA + "6. Logout" + RESET);
            System.out.println(GREEN + "===================================" + RESET);
            System.out.print(YELLOW + "Choose an option: " + RESET);
            int ch = Integer.parseInt(sc.nextLine());

            if (ch == 1) {
                // Create new playlist
                System.out.print("Enter playlist name: ");
                String name = sc.nextLine();
                playlists.add(name);
                System.out.println("Playlist '" + name + "' created successfully!");
            } else if (ch == 2) {
                // Add song to playlist
                if (playlists.head == null) System.out.println("No playlists available.");
                else {
                    System.out.println("Your playlists:");
                    playlists.display(username);
                    System.out.print(YELLOW + "Choose a playlist (1-N): " + RESET);
                    int idx = Integer.parseInt(sc.nextLine());
                    Playlist p = playlists.get(idx);
                    if (p == null) System.out.println("Invalid selection.");
                    else {
                        System.out.print(YELLOW + "Enter song name: " + RESET);
                        String t = sc.nextLine();
                        System.out.print(YELLOW + "Enter artist name: " + RESET);
                        String a = sc.nextLine();
                        p.addSong(t, a);
                        p.saveToFile(username);
                        System.out.println(t + " by " + a + " added successfully to " + p.name);
                    }
                }
            } else if (ch == 3) {
                // Remove song from playlist
                System.out.println("Your playlists:");
                playlists.display(username);
                System.out.print(YELLOW + "Choose a playlist: " + RESET);
                int idx = Integer.parseInt(sc.nextLine());
                Playlist p = playlists.get(idx);
                if (p != null) {
                    p.loadFromFile(username);
                    p.displaySongs();
                    int s = getIntInput(YELLOW + "Enter song number to remove: " + RESET);

                    if (p.removeSong(s)) {
                        p.saveToFile(username);
                        System.out.println("Song removed.");
                    } else System.out.println("Invalid song number.");
                }
            } else if (ch == 4) {
                // Play a song from playlist
                playlists.display(username);
                System.out.print(YELLOW + "Choose a playlist: " + RESET);
                int idx = Integer.parseInt(sc.nextLine());
                Playlist p = playlists.get(idx);
                if (p != null) {
                    p.loadFromFile(username);
                    p.displaySongs();
                    System.out.print(YELLOW + "Choose a song to play: " + RESET);
                    int s = Integer.parseInt(sc.nextLine());
                    Song song = p.getSong(s);
                    if (song != null) System.out.println("Now playing: " + song.title + " by " + song.artist);
                    else System.out.println("Invalid song selection.");
                }
            } else if (ch == 5) {
                // Update (rename) a song in a playlist
                updatePlaylist(username, playlists);
            } else if (ch == 6) {
                // Logout
                System.out.println(YELLOW + "Logged out successfully." + RESET);
                break;
            } else System.out.println("Invalid choice.");
        }
    }

    // Menu for admin users (user and playlist management)
    static void adminMenu(UserList users) throws IOException {
        String CYAN = "\u001B[36m";
        String YELLOW = "\u001B[33m";
        String GREEN = "\u001B[32m";
        String MAGENTA = "\u001B[35m";
        String RESET = "\u001B[0m";
        while (true) {
            System.out.println(GREEN + "\n===================================" + RESET);
            System.out.println(CYAN + "=== Admin Menu ===" + RESET);
            System.out.println(GREEN + "===================================" + RESET);
            System.out.println(MAGENTA + "1. View all users" + RESET);
            System.out.println(MAGENTA + "2. Remove a user" + RESET);
            System.out.println(MAGENTA + "3. View all playlists" + RESET);
            System.out.println(MAGENTA + "4. View all songs in a playlist" + RESET);
            System.out.println(MAGENTA + "5. Logout" + RESET);
            System.out.println(GREEN + "===================================" + RESET);
            System.out.print(YELLOW + "Choose an option: " + RESET);
            int ch = Integer.parseInt(sc.nextLine());

            if (ch == 1) {
                // Display all users
                System.out.println("\n=== All Users ===");
                users.display();
            } else if (ch == 2) {
                // Remove a user
                users.display();
                System.out.print(YELLOW + "Choose user to remove: " + RESET);
                int idx = Integer.parseInt(sc.nextLine());
                if (users.remove(idx)) {
                    users.save();
                    System.out.println("User removed.");
                } else System.out.println("Invalid selection.");
            } else if (ch == 3) {
                // View all playlists for all users
                System.out.println("\n=== All Playlists ===");
                User temp = users.head;
                while (temp != null) {
                    System.out.println("\nUser: " + temp.username);
                    String currentUsername = temp.username;
                    File[] files = new File(".").listFiles((dir, name) -> name.startsWith("playlist-" + currentUsername));

                    int i = 1;
                    for (File f : files) {
                        String name = f.getName().replace("playlist-" + temp.username + "-", "").replace(".txt", "");
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        int count = 0;
                        while (br.readLine() != null) count++;
                        br.close();
                        System.out.println(i++ + ". " + name + " (Songs: " + count + ")");
                    }
                    temp = temp.next;
                }
            } else if (ch == 4) {
                // View all songs in a selected playlist for a user
                System.out.println("\n=== View Songs in Playlist ===");
                users.display();
                System.out.print(YELLOW + "Choose a user: " + RESET);
                int uidx = Integer.parseInt(sc.nextLine());
                User u = users.get(uidx);
                if (u == null) System.out.println("Invalid user selection.");
                else {
                    File[] files = new File(".").listFiles((dir, name) -> name.startsWith("playlist-" + u.username));
                    int i = 1;
                    for (File f : files) {
                        String name = f.getName().replace("playlist-" + u.username + "-", "").replace(".txt", "");
                        System.out.println(i++ + ". " + name);
                    }
                    System.out.print("Choose a playlist: ");
                    int pidx = Integer.parseInt(sc.nextLine());
                    if (pidx <= 0 || pidx > files.length) System.out.println("Invalid playlist.");
                    else {
                        BufferedReader br = new BufferedReader(new FileReader(files[pidx - 1]));
                        String line;
                        int j = 1;
                        while ((line = br.readLine()) != null) {
                            Song s = Song.fromString(line);
                            System.out.println(j++ + ". " + s.title + " by " + s.artist);
                        }
                        br.close();
                    }
                }
            } else if (ch == 5) {
                // Logout
                System.out.println(YELLOW + "Logged out successfully." + RESET);
                break;
            } else System.out.println("Invalid option.");
        }
    }

    // Saves all playlists to files (not used in main flow)
    private static void savePlaylists(PlaylistList playlists) throws IOException {
        Playlist temp = playlists.head;
        while (temp != null) {
            temp.saveToFile(temp.name);
            temp = temp.next;
        }
    }

    // Loads all playlists for a user from files (not used in main flow)
    private static void loadPlaylists(PlaylistList playlists, String username) throws IOException {
        File folder = new File(".");
        File[] files = folder.listFiles((dir, name) -> name.startsWith("playlist-" + username + "-") && name.endsWith(".txt"));

        if (files == null) return;

        for (File file : files) {
            String playlistName = file.getName().replace("playlist-" + username + "-", "").replace(".txt", "");
            Playlist playlist = new Playlist(playlistName);
            
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                Song song = Song.fromString(line);
                playlist.addSong(song.title, song.artist);
            }
            br.close();

            if (playlists.head == null) playlists.head = playlist;
            else {
                Playlist temp = playlists.head;
                while (temp.next != null) temp = temp.next;
                temp.next = playlist;
            }
        }
    }
}