package com.teamwork.animalshelter.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Menu implements Askable{
    private ItemMenu itemMenu;
    private String error;
    private String name;
    private int interval;

    private boolean waitingResponse;

    private Menu(String name, int interval) {
        this.name = name;
        this.interval = interval;
        this.error = "";
        this.waitingResponse = false;
    }

    class ItemMenu {
        private String label;
        private String name;
        private ItemMenu parent;
        private List<ItemMenu> childs;
        private boolean command;
        private String commandName;

        public String getLabel() {
            return label;
        }

        public String getName() {
            return name;
        }

        public ItemMenu getParent() {
            return parent;
        }

        public List<ItemMenu> getChilds() {
            return childs;
        }

        public ItemMenu(String label, String name, ItemMenu parent) {
            this.label = label;
            this.name = name;
            this.parent = parent;
            this.childs = new ArrayList<>();
        }

        public boolean isCommand() {
            return command;
        }

        public void setCommand(boolean command) {
            this.command = command;
        }

        public String getCommandName() {
            return commandName;
        }

        public void setCommandName(String commandName) {
            this.commandName = commandName;
        }

        public void addChild(ItemMenu item) {
            childs.add(item);
        }
    }

    private String getError() {
        return error;
    }

    private void setError(String error) {
        this.error = error;
    }

    @Override
    public String getName() {
        return name;
    }

    private int getInterval() {
        return interval;
    }

    private void setRootItemMenu() {
        while(itemMenu.getParent() != null) {
            itemMenu = itemMenu.getParent();
        }
    }

    private void setItemMenu(ItemMenu itemMenu) {
        this.itemMenu = itemMenu;
    }

    private boolean isCommand() {
        return itemMenu.isCommand();
    }

    @Override
    public void init() {
        setError("");
        setRootItemMenu();
    }

    @Override
    public boolean empty() {
        return isCommand();
    }

    @Override
    public String nextAction() {
        if (itemMenu.getChilds().size() == 0) {
            // должно быть исключение !!!
            // "конечная ветка меню должна содержать команду"
        }
        StringBuilder result = new StringBuilder();
        List<ItemMenu> childs = itemMenu.getChilds();
        for(int i=0; i < childs.size(); i++) {
            result.append(childs.get(i));
            result.append('\n');
        }
        return result.toString();
    }

    @Override
    public void setResponse(String response) {
        boolean isFound = false;
        for (ItemMenu item : itemMenu.getChilds()) {
            if (response.equals(item.getLabel())) {
                setItemMenu(item);
                isFound = true;
                break;
            }
        }
        if (!isFound) {
            // исключение!!!
            // "не найден пункт меню"
        }
    }

    @Override
    public boolean checkResponse(String response) {
        boolean result = false;
        setError("Неверно указан номер пункта меню.");
        for (ItemMenu item : itemMenu.getChilds()) {
            if (response.equals(item.getLabel())) {
                result = true;
                setError("");
                break;
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getResult() {
        Map<String, String> result = new HashMap<>();
        result.put("command", itemMenu.getCommandName());
        return result;
    }

    @Override
    public boolean verificationRequired() {
        return true;
    }

    @Override
    public String getLastError() {
        return getError();
    }

    @Override
    public void setWaitingResponse(boolean waitingResponse) {
        this.waitingResponse = waitingResponse;
    }

    @Override
    public boolean isWaitingResponse() {
        return this.waitingResponse;
   }

    @Override
    public boolean intervalExceeded(int minutes) {
        return minutes > getInterval();
    }

    public static Menu load(String filePath) {

    }

    public Menu dublicate() {

    }
}
