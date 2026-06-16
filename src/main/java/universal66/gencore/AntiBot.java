package universal66.gencore;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Arrays;
import java.util.List;

public final class AntiBot implements Listener {
    private static final List<String> BOT_NICK_LIST = Arrays.stream("""
            Vxydias
            Orafik
            urbadong
            Emilafacan
            LeiteComBiclacha
            NimbleOx3320468
            PearlPlate08452
            PenguinSk
            pekis542
            WARTORTLE69
            GamerBoy41999
            PluckyShoe51299
            alternateprxsma
            olaf_wojtekk
            mejem
            Omoshiruii
            lampa_gg
            PearlPlate08452
            WalterWhite56
            HooligansAlt7
            xianyvgan
            Panpj2011
            6tar
            lazerbread
            xStrqfje
            pekis542
            urpoint
            Pugifying
            godblesseddd
            Manequin
            SatinyBog680024
            Ingaroemil
            keairr
            NockPC
            ReminiscenceSMP
            """.trim().replace("\r\n", "\n").replace("\r", "\n").split("\n")).toList();

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (BOT_NICK_LIST.contains(event.getName())) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.setKickMessage("Join disallowed");

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "banip " + event.getAddress().getHostAddress());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + event.getName());
        }
    }
}
