package potentiamc.com;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EnumEntityUseAction;
import net.minecraft.server.v1_7_R4.MathHelper;
import net.minecraft.server.v1_7_R4.NetworkManager;
import net.minecraft.server.v1_7_R4.Packet;
import net.minecraft.server.v1_7_R4.PacketPlayInUseEntity;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityStatus;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.handler.codec.MessageToMessageDecoder;

/*
 * Made for orion if u steal it ur a shit dev
 */

public class Async extends JavaPlugin implements Listener {

	private Map<String, Long> delay = new HashMap<>();

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	private static int TICKS = 5;
	
	public void inject(Player p) {
		CraftPlayer cp = (CraftPlayer) p;
		NetworkManager network = cp.getHandle().playerConnection.networkManager;

		Channel channel = null;
		try {
			Field field = NetworkManager.class.getDeclaredField("m");
			field.setAccessible(true);
			channel = (Channel) field.get(network);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		
		channel.pipeline().addAfter("decoder", "potentia", new MessageToMessageDecoder() {

			private boolean readPacket(Object objectPacket) {

				Packet packet = (Packet) objectPacket;

				if (packet instanceof PacketPlayInUseEntity) {

					PacketPlayInUseEntity pI = (PacketPlayInUseEntity) packet;
					Entity e = pI.a(((CraftWorld) p.getWorld()).getHandle());

					if (((CraftWorld) p.getWorld()).getHandle().players.contains(e)
							&& pI.c() == EnumEntityUseAction.ATTACK) {
						Player damaged = (Player) e.getBukkitEntity();
						
						if(delay.containsKey(p.getName())) {
							
							long s = ( (System.currentTimeMillis() - delay.get(p.getName())) / 1000  ) * 20;
							System.out.println(s);

							if(s <  5) {
								return false;
							}
							
							
							
						}

						PacketPlayOutEntityStatus status = new PacketPlayOutEntityStatus(e, (byte) 2);
						
						delay.put(p.getName(), System.currentTimeMillis());

						runAsync(p, e, damaged, status);

						return false;
					}

				}

				return true;
			}

			protected void decode(ChannelHandlerContext c, Object packet, List list) throws Exception {
				list.add(packet);

				if (!readPacket(packet)) {
					list.remove(packet);
					c.flush();
				}

			}

		});

	}
	
	
	public void deinject(Channel channel) {
		if(channel.pipeline().get("potentia") != null) {
			channel.pipeline().remove("potentia");
		}
	}

	public static double kbX = 0.95;
	public static double kbY = 0.95;
	public static double kbZ = 0.95;

	public void runAsync(Player attacker, Entity e, Player dmgd, Packet packet) {
		new BukkitRunnable() {
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers()) {
					CraftPlayer cp = (CraftPlayer) p;

					EntityPlayer ep = ((CraftPlayer) dmgd).getHandle();
					EntityPlayer epa = ((CraftPlayer) attacker).getHandle();

					ep.setHealth(ep.getHealth() - 1);

					double victimMotX = e.motX;
					double victimMotY = e.motY;
					double victimMotZ = e.motZ;
					
					ep.velocityChanged = true;

					g(ep, -MathHelper.sin(epa.yaw * 3.1415927F / 180.0F) * 1 * 0.5F * 0.95D * kbX, 0.1D * kbY,
							MathHelper.cos(epa.yaw * 3.1415927F / 180.0F) * 1 * 0.5F * 0.95D * kbZ);

					//ep.motX *= 1.05;
					// ep.motY *= 1.05;
					//ep.motZ *= 1.05;

		            PlayerVelocityEvent event = new PlayerVelocityEvent(ep.getBukkitEntity(), ep.getBukkitEntity().getVelocity());
		            Bukkit.getPluginManager().callEvent(event);
		            if (!event.isCancelled())
		            {
		              ep.getBukkitEntity().setVelocity(event.getVelocity());
		            }
		            
		            ep.velocityChanged = false;
		            ep.motX = victimMotX;
		            ep.motY = victimMotY;
		            ep.motZ = victimMotZ;
					
					cp.getHandle().playerConnection.sendPacket(packet);

				}

			}
		}.runTaskAsynchronously(this);
	}

	public void g(EntityPlayer e, double d0, double d1, double d2) {
		e.motX += d0;
		e.motY += d1;
		e.motZ += d2;
		e.al = true;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		inject(event.getPlayer());
		
	}

}
