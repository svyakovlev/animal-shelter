package com.teamwork.animalshelter.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.teamwork.animalshelter.exception.ErrorMenu;
import com.teamwork.animalshelter.exception.NotFoundAttributeXmlFile;
import com.teamwork.animalshelter.exception.NotFoundElementXmlFile;
import com.teamwork.animalshelter.parser.Element;
import com.teamwork.animalshelter.parser.ParserXML;

/**
 * Класс определяет работу меню
 * <br>
 * <ul>
 * Данные класса:
 * <br>
 * <li>{@code itemMenu} - текущий элемент меню;</li>
 * <li>{@code error} - ошибка, возникшая при проверке последнего ответа пользователя;</li>
 * <li>{@code name} - название меню;</li>
 * <li>{@code interval} - задаваемый интервал ожидания (в минутах) ответа от пользователя. При превышении этого интервала происходит сброс команды;</li>
 * <li>{@code waitingResponse} - {@code true}, если режим ожидания включен;</li>
 * </ul>
 * @see Askable
 * @see ParserXML
 */
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

    /**
     * Класс определяет отдельный пункт меню
     * <br>
     * <ul>
     * Данные класса:
     * <br>
     * <li>{@code label} - метка пункта меню. Анализируется в ответе пользователя;</li>
     * <li>{@code name} - название пункта меню;</li>
     * <li>{@code parent} - родительский пункт меню;</li>
     * <li>{@code childs} - дочерние пункты меню;</li>
     * <li>{@code command} - {@code true}, если данный пункт меню является командой;</li>
     * <li>{@code commandName} - название команды (имеет смысл, если {@code command} установлена в {@code true} ;</li>
     * </ul>
     * @see Menu
     */
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

        public boolean getCommand() {
            return this.command;
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
        setWaitingResponse(false);
    }

    @Override
    public boolean empty() {
        return isCommand();
    }

    @Override
    public String nextAction() {
        if (itemMenu.getChilds().size() == 0) {
            if (!isCommand()) {
                throw new ErrorMenu(getName(), "Конечный пункт меню должен содержать команду");
            }
            return null;
        } else {
            if (isCommand()) {
                throw new ErrorMenu(getName(), "Промежуточный пункт меню НЕ должен содержать команду");
            }
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
            throw new ErrorMenu(getName(), String.format("Не найден пункт меню '%s'", response));
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

    /**
     * Создает новый объект {@code Menu} из файла XML
     * @param parserXML объект, с помощью которого будет производится парсинг XML
     * @param file файловый объект
     * @return {@code Menu} возвращает новый объект
     * @see ParserXML
     */
    public static Menu load(ParserXML parserXML, File file) {
        Element root = parserXML.parse(file);

        String intervalString = root.getAttributes().get("interval");
        if (intervalString == null) {
            throw new NotFoundAttributeXmlFile(root.getName(), "interval");
        }

        String name = root.getAttributes().get("name");
        if (name == null) {
            throw new NotFoundAttributeXmlFile(root.getName(), "name");
        }

        Menu menu;
        try {
            menu = new Menu(name, Integer.valueOf(intervalString));
        } catch (NumberFormatException e) {
            throw new ErrorMenu(name, "Ошибка в указании интервала");
        } catch (Exception e) {
            throw new ErrorMenu(name, "Ошибка при создании меню. " + e.getMessage());
        }

        ItemMenu rootItemMenu = menu.createItemMenu(root, null);
        if (rootItemMenu == null) {
            throw new ErrorMenu(name, "Ошибка получения корневого элемента меню");
        }
        menu.setItemMenu(rootItemMenu);
        return menu;
    }

    /**
     * Элементы меню создаются при помощи рекурсивного вызова
     * @param element обобщенный элемент, на базе которого создается элемент меню
     * @param parent родитель для создаваемого элемента меню
     * @return элемент меню
     */
    private ItemMenu createItemMenu(Element element, ItemMenu parent) {
        Map<String, String> attributes = element.getAttributes();
        List<Element> childs = element.getChilds();
        String name = null;
        String label = null;
        String command = null;
        ItemMenu pointMenu = null;

        switch (element.getName()) {
            case "Menu":
                if (!attributes.containsKey("name"))
                    throw new NotFoundAttributeXmlFile(element.getName(), "name");
                name = element.getAttributes().get("name");
                pointMenu = new ItemMenu("", name, parent);
                break;
            case "ItemMenu":
                if (!attributes.containsKey("name"))
                    throw new NotFoundAttributeXmlFile(element.getName(), "name");
                if (!attributes.containsKey("label"))
                    throw new NotFoundAttributeXmlFile(element.getName(), "label");
                if (childs.isEmpty() && !attributes.containsKey("command"))
                    throw new NotFoundAttributeXmlFile(element.getName(), "command", attributes.get("label"));
                name = element.getAttributes().get("name");
                label = element.getAttributes().get("label");
                command = element.getAttributes().get("command");
                pointMenu = new ItemMenu(label, name, parent);
                break;
            default:
                throw new NotFoundElementXmlFile(element.getName(), "Menu");
        }

        if (command != null) {
            pointMenu.setCommand(true);
            pointMenu.setCommandName(command);
            return pointMenu;
        }
        for (int i=0; i < childs.size(); i++) {
            pointMenu.addChild(createItemMenu(childs.get(i), pointMenu));
        }
        return pointMenu;
    }

    /**
     * Создает полную копию текущего объекта {@code Menu}
     * @return {@code Menu} возвращает новый объект
     */
    @Override
    public Menu dublicate() {
        Menu newMenu = new Menu(getName(), getInterval());
        setRootItemMenu();
        newMenu.setItemMenu(dublicateItemMenu(itemMenu, null));
        return newMenu;
    }

    private ItemMenu dublicateItemMenu(ItemMenu pointMenu, ItemMenu parent) {
        ItemMenu newItemMenu = new ItemMenu(pointMenu.getLabel(), pointMenu.getName(), parent);
        newItemMenu.setCommand(pointMenu.getCommand());
        newItemMenu.setCommandName(pointMenu.getCommandName());
        List<ItemMenu> childs = pointMenu.getChilds();
        for (int i = 0; i < childs.size(); i++) {
            newItemMenu.addChild(dublicateItemMenu(childs.get(i), newItemMenu));
        }
        return newItemMenu;
    }
}
