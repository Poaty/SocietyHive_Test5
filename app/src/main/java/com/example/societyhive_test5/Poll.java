package com.example.societyhive_test5;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Poll {
    private String id;
    private String title;
    private Timestamp endsAt;
    private String question;
    private List<String> options;
    private String societyId;
    private boolean isActive;


    private String societyName = "";


    private int selectedOptionIndex = -1; // -1 means nothing picked yet
    private boolean hasVoted = false;
    private int votedOptionIndex = -1;
    private List<Integer> voteCounts = new ArrayList<>();
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

    public Timestamp getEndsAt() { return endsAt; }
    public void setEndsAt(Timestamp endsAt) { this.endsAt = endsAt; }


    // poll is closed if endsAt is in the past
    public boolean isClosed() {
        return endsAt != null && endsAt.toDate().before(new Date());
    }
}
