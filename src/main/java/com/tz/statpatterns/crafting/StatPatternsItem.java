package com.tz.statpatterns.crafting;

import ae2.crafting.pattern.EncodedPatternItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public final class StatPatternsItem extends EncodedPatternItem<StatPatternsDetails> {
    public StatPatternsItem() { super(StatPatternsDetails::decode, null, 64); }
    @Override protected void getCheckedSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this));
    }
    @Override @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    protected void addCheckedInformation(ItemStack stack, net.minecraft.world.World world,
            java.util.List<String> lines, net.minecraft.client.util.ITooltipFlag flags) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(StatPatternsDetails.TAG, 10))
            super.addCheckedInformation(stack, world, lines, flags);
    }
}

