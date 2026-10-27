package atomicstryker.infernalmobs.common.mods;

import atomicstryker.infernalmobs.common.MobModifier;

public class MM_Unyielding extends MobModifier {

    private static String[] suffix = {"ofRelentlessness", "theUnYielding", "theUnstoppable"};
    private static String[] prefix = {"relentless", "unyielding", "unstoppable"};

    public MM_Unyielding() {
        super();
    }

    public MM_Unyielding(MobModifier next) {
        super(next);
    }

    @Override
    public String getModName() {
        return "Unyielding";
    }

    @Override
    public boolean onKnockBack() {
        return true;
    }

    @Override
    protected String[] getModNameSuffix() {
        return suffix;
    }

    @Override
    protected String[] getModNamePrefix() {
        return prefix;
    }

}
