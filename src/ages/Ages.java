package ages;

import ages.content.*;
import mindustry.mod.*;
import ages.gen.*;

public class Ages extends Mod{
    @Override
    public void loadContent(){
        EntityRegistry.register();
        AgBlocks.load();
    }
}
