package com.example.societyhive_test5;

import java.util.ArrayList;
import java.util.List;

/**
 * Poll model.
 *
 * Firestore document structure:  polls/{pollId}
 *   title       (String)
 *   question    (String)
 *   options     (Array<String>)
 *   societyId   (String)
 *   isActive    (boolean)
 *
 * Votes stored at:  polls/{pollId}/votes/{userId}
 *   optionIndex (number)
 *   votedAt     (Timestamp)
 */
public class Poll {
    private String id;
    private String title;
    private String question;
    private List<String> options;
    private String societyId;
    private boolean isActive;

    // Fetched separately — not stored in the poll document
    private String societyName = "";

    // UI-only state
    private int selectedOptionIndex = -1; // -1 = nothing tapped yet
    private boolean hasVoted = false;
    private int votedOptionIndex = -1;
    private List<Integer> voteCounts = new ArrayList<>(); // votes per option index
    private int totalVotes = 0;

    public Poll() {}

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getQuestion() { return question != null ? question : ""; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getOptions() { return options != null ? options : new ArrayList<>(); }
    public void setOptions(List<String> options) { this.options = options; }

    public String getSocietyId() { return societyId != null ? societyId : ""; }
    public void setSocietyId(String societyId) { this.societyId = societyId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getSelectedOptionIndex() { return selectedOptionIndex; }
    public void setSelectedOptionIndex(int i) { selectedOptionIndex = i; }

    public boolean isHasVoted() { return hasVoted; }
    public void setHasVoted(boolean hasVoted) { this.hasVoted = hasVoted; }

    public int getVotedOptionIndex() { return votedOptionIndex; }
    public void setVotedOptionIndex(int i) { this.votedOptionIndex = i; }

    public List<Integer> getVoteCounts() { return voteCounts; }
    public void setVoteCounts(List<Integer> voteCounts) { this.voteCounts = voteCounts; }

    public int getTotalVotes() { return totalVotes; }
    public void setTotalVotes(int totalVotes) { this.totalVotes = totalVotes; }

    public String getSocietyName() { return societyName; }
    public void setSocietyName(String societyName) { this.societyName = societyName != null ? societyName : ""; }
}
