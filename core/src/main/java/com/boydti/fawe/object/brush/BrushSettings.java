package com.boydti.fawe.object.brush;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.brush.scroll.ScrollAction;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.command.CommandCallable;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.ProcessedCallable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BrushSettings {
    public enum SettingType {
        BRUSH,
        SIZE,
        MASK,
        SOURCE_MASK,
        TRANSFORM,
        FILL,
        PERMISSIONS,
        SCROLL_ACTION,
    }

    private Map<SettingType, Object> constructor = new ConcurrentHashMap<>();

    private Brush brush = null;
    private Mask mask = null;
    private Mask sourceMask = null;
    private ResettableExtent transform = null;
    private Pattern material;
    private double size = 1;
    private Set<String> permissions;
    private ScrollAction scrollAction;

    public BrushSettings() {
        this.permissions = new HashSet<>();
        this.constructor.put(SettingType.PERMISSIONS, permissions);
    }

    public static BrushSettings get(BrushTool tool, Player player, LocalSession session, Map<String, Object> settings) throws CommandException, InputParseException {
        Dispatcher dispatcher = CommandManager.getInstance().getDispatcher();
        Dispatcher brushDispatcher = (Dispatcher) (dispatcher.get("brush").getCallable());
        if (brushDispatcher == null) {
            return null;
        }
        String constructor = (String) settings.get(SettingType.BRUSH.name());
        if (constructor == null) {
            return new BrushSettings();
        }
        String[] split = constructor.split(" ");

        CommandCallable sphereCommand = ((ProcessedCallable) brushDispatcher.get(split[0]).getCallable()).getParent();
        CommandLocals locals = new CommandLocals();
        locals.put(Actor.class, player);
        String args = constructor.substring(constructor.indexOf(' ') + 1);
        String[] parentArgs = new String[]{"brush", split[0]};
        BrushSettings bs = (BrushSettings) sphereCommand.call(args, locals, parentArgs);
        if (settings.containsKey(SettingType.PERMISSIONS.name())) {
            bs.permissions.addAll((Collection<? extends String>) settings.get(SettingType.PERMISSIONS.name()));
        }
        if (settings.containsKey(SettingType.SIZE.name())) {
            bs.size = (double) settings.get(SettingType.SIZE.name());
        }

        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);

        if (settings.containsKey(SettingType.MASK.name())) {
            String maskArgs = (String) settings.get(SettingType.MASK.name());
            Mask mask = WorldEdit.getInstance().getMaskFactory().parseFromInput(maskArgs, parserContext);
            bs.setMask(mask);
            bs.constructor.put(SettingType.MASK, maskArgs);
        }
        if (settings.containsKey(SettingType.SOURCE_MASK.name())) {
            String maskArgs = (String) settings.get(SettingType.SOURCE_MASK.name());
            Mask mask = WorldEdit.getInstance().getMaskFactory().parseFromInput(maskArgs, parserContext);
            bs.setSourceMask(mask);
            bs.constructor.put(SettingType.SOURCE_MASK, maskArgs);
        }
        if (settings.containsKey(SettingType.TRANSFORM.name())) {
            String transformArgs = (String) settings.get(SettingType.TRANSFORM.name());
            ResettableExtent extent = Fawe.get().getTransformParser().parseFromInput(transformArgs, parserContext);
            bs.setTransform(extent);
            bs.constructor.put(SettingType.TRANSFORM, transformArgs);
        }
        if (settings.containsKey(SettingType.FILL.name())) {
            String fillArgs = (String) settings.get(SettingType.FILL.name());
            Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(fillArgs, parserContext);
            bs.setFill(pattern);
            bs.constructor.put(SettingType.FILL, fillArgs);
        }
        if (settings.containsKey(SettingType.SCROLL_ACTION.name())) {
            String actionArgs = (String) settings.get(SettingType.SCROLL_ACTION.name());
            ScrollAction action = ScrollAction.fromArguments(tool, player, session, actionArgs, false);
            if (action != null) {
                bs.setScrollAction(action);
                bs.constructor.put(SettingType.SCROLL_ACTION, actionArgs);
            }
        }
        return bs;
    }

    public BrushSettings setBrush(Brush brush) {
        this.brush = brush;
        return this;
    }

    public BrushSettings clear() {
        brush = null;
        mask = null;
        sourceMask = null;
        transform = null;
        material = null;
        scrollAction = null;
        size = 1;
        permissions.clear();
        constructor.clear();
        return this;
    }

    public BrushSettings addSetting(SettingType type, String args) {
        constructor.put(type, args);
        return this;
    }

    public Map<SettingType, Object> getSettings() {
        return Collections.unmodifiableMap(constructor);
    }

    public BrushSettings setMask(Mask mask) {
        if (mask == null) constructor.remove(SettingType.MASK);
        this.mask = mask;
        return this;
    }

    public BrushSettings setSourceMask(Mask mask) {
        if (mask == null) constructor.remove(SettingType.SOURCE_MASK);
        this.sourceMask = mask;
        return this;
    }

    public BrushSettings setTransform(ResettableExtent transform) {
        if (transform == null) constructor.remove(SettingType.TRANSFORM);
        this.transform = transform;
        return this;
    }

    public BrushSettings setFill(Pattern pattern) {
        if (pattern == null) constructor.remove(SettingType.FILL);
        this.material = pattern;
        return this;
    }

    public BrushSettings setSize(double size) {
        this.size = size;
        if (size == -1) {
            constructor.remove(SettingType.SIZE);
        } else {
            constructor.put(SettingType.SIZE, size);
        }
        return this;
    }

    public BrushSettings setScrollAction(ScrollAction scrollAction) {
        if (scrollAction == null) constructor.remove(SettingType.SCROLL_ACTION);
        this.scrollAction = scrollAction;
        return this;
    }

    public BrushSettings addPermission(String permission) {
        this.permissions.add(permission);
        return this;
    }

    public BrushSettings addPermissions(String... perms) {
        for (String perm : perms) permissions.add(perm);
        return this;
    }

    public Brush getBrush() {
        return brush;
    }

    public Mask getMask() {
        return mask;
    }

    public Mask getSourceMask() {
        return sourceMask;
    }

    public ResettableExtent getTransform() {
        return transform;
    }

    public Pattern getMaterial() {
        return material;
    }

    public double getSize() {
        return size;
    }

    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public ScrollAction getScrollAction() {
        return scrollAction;
    }

    public boolean canUse(Actor actor) {
        for (String perm : getPermissions()) {
            if (actor.hasPermission(perm)) return true;
        }
        return false;
    }

}
