package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most once group
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(String name, String mobile) throws Exception {
        //If the mobile number exists in database, throw "User already exists" exception
        //Otherwise, create the user and return "SUCCESS"
        //your code here
        if(userMobile.contains(mobile)) throw new RuntimeException("User already exists");
        User user = new User(name,mobile);
        userMobile.add(mobile);
        return "SUCCESS";
    }

    public Group createGroup(List<User> users){
        // The list contains at least 2 users where the first user is the admin.
        // If there are only 2 users, the group is a personal chat and the group name should be kept as the name of the second user(other than admin)
        // If there are 2+ users, the name of group should be "Group customGroupCount". For example, the name of first group would be "Group 1", second would be "Group 2" and so on.
        // If group is successfully created, return group.
        //your code here
        if (users == null || users.size() < 2) {
            throw new IllegalArgumentException("There must be at least 2 users to create a group.");
        }
        int len = users.size();
        Group group = new Group();
        if(len == 2){
            group.setName(users.get(1).getName());
        }
        else{
            customGroupCount++;
            group.setName("Group "+customGroupCount);
        }
        group.setNumberOfParticipants(len);
        groupUserMap.put(group,new ArrayList<>(users));
        groupMessageMap.put(group,new ArrayList<>());
        adminMap.put(group,users.get(0));
        return group;
    }

    public int createMessage(String content){
        // The 'i^th' created message has message id 'i'.
        // Return the message id.
        //your code here
        Message message = new Message(messageId,content,new Date());
        messageId++;
        return  message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        //If the message is sent successfully, return the final number of messages in that group.
        //your code here
        if (!groupUserMap.containsKey(group)) throw new RuntimeException("Group does not exist");


        // Check if the sender is a member of the group
        List<User> userList = groupUserMap.get(group);

        if (!userList.contains(sender)) throw new RuntimeException("You are not allowed to send message");

        List<Message> messageList = groupMessageMap.get(group);
        messageList.add(message);

        senderMap.put(message,sender);
        groupMessageMap.put(group,messageList);

        return messageList.size();

    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        //Throw "User is not a participant" if the user is not a part of the group
        //Change the admin of the group to "user" and return "SUCCESS".

        //your code here
        if (!groupUserMap.containsKey(group)) throw new RuntimeException("Group does not exist");
        if(adminMap.get(group) != approver) throw new RuntimeException("Approver does not have rights");
        if(groupUserMap.get(group).contains(user)) throw new RuntimeException("User is not a participant");

        adminMap.put(group,user);

        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception{
        //If user is not found in any group, throw "User not found" exception
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        //your code here
        boolean userFound = false;
        Group userGroup = null;

        // Find the group the user is part of
        for (Group group : groupUserMap.keySet()) {
            if (groupUserMap.get(group).contains(user)) {
                userFound = true;
                userGroup = group;
                break;
            }
        }

        if (!userFound) {
            throw new RuntimeException("User not found");
        }

        if (adminMap.get(userGroup).equals(user)) {
            throw new RuntimeException("Cannot remove admin");
        }

        // Remove the user from the group
        List<User> userList = groupUserMap.get(userGroup);
        userList.remove(user);
        groupUserMap.put(userGroup, userList);

        List<Message> messageList = groupMessageMap.get(userGroup);
        List<Message> messagesToRemove = new ArrayList<>();

        for (Message message : messageList) {
            if (senderMap.get(message).equals(user)) {
                messagesToRemove.add(message);
                senderMap.remove(message);
            }
        }

        messageList.removeAll(messagesToRemove);
        groupMessageMap.put(userGroup, messageList);

        // Calculate the updated numbers
        int updatedNumberOfUsers = userList.size();
        int updatedNumberOfMessagesInGroup = messageList.size();
        int updatedNumberOfOverallMessages = senderMap.size();

        return updatedNumberOfUsers + updatedNumberOfMessagesInGroup + updatedNumberOfOverallMessages;
    }

    public String findMessage(Date start, Date end, int K) throws Exception{
        // Find the Kth latest message between start and end (excluding start and end)
        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception
        //your code here
        List<Message> messagesInRange = new ArrayList<>();

        // Collect all messages within the specified date range
        for (List<Message> messages : groupMessageMap.values()) {
            for (Message message : messages) {
                if (message.getTimestamp().after(start) && message.getTimestamp().before(end)) {
                    messagesInRange.add(message);
                }
            }
        }

        // Sort messages by their timestamps in descending order
        messagesInRange.sort((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()));

        // Check if the number of messages is at least K
        if (messagesInRange.size() < K) {
            throw new Exception("K is greater than the number of messages");
        }

        // Return the Kth latest message
        return messagesInRange.get(K - 1).getContent();
    }
}