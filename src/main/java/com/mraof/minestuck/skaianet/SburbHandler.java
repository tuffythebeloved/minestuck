package com.mraof.minestuck.skaianet;

import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.advancements.MSCriteriaTriggers;
import com.mraof.minestuck.entry.EntryProcess;
import com.mraof.minestuck.item.MSItems;
import com.mraof.minestuck.item.crafting.alchemy.GristType;
import com.mraof.minestuck.item.crafting.alchemy.GristTypes;
import com.mraof.minestuck.network.MSPacketHandler;
import com.mraof.minestuck.network.TitleSelectPacket;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.IdentifierHandler;
import com.mraof.minestuck.player.PlayerIdentifier;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.util.ColorHandler;
import com.mraof.minestuck.world.lands.LandTypePair;
import com.mraof.minestuck.world.lands.LandTypes;
import com.mraof.minestuck.world.lands.terrain.TerrainLandType;
import com.mraof.minestuck.world.lands.title.TitleLandType;
import com.mraof.minestuck.world.storage.PlayerData;
import com.mraof.minestuck.world.storage.PlayerSavedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A class for managing sburb-related stuff from outside this package that is dependent on connections and sessions.
 * For example: Titles, land aspects, entry items etc.
 * @author kirderf1
 */
public final class SburbHandler
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	static Map<PlayerEntity, Vec3d> playersInTitleSelection = new HashMap<>();	//TODO Consider making this non-static
	
	private static Title produceTitle(World world, PlayerIdentifier player)
	{
		Session session = SessionHandler.get(world).getPlayerSession(player);
		if(session == null)
			if(MinestuckConfig.SERVER.playerSelectedTitle.get())
				session = new Session();
			else
			{
				LOGGER.warn("Trying to generate a title for {} before creating a session!", player.getUsername(), new Throwable().fillInStackTrace());
				return null;
			}
		
		Title title = null;
		if(session.predefinedPlayers.containsKey(player))
		{
			PredefineData data = session.predefinedPlayers.get(player);
			title = data.getTitle();
		}
		
		if(title == null)
		{
			try
			{
				title = Generator.generateTitle(session, EnumAspect.valuesSet(), player);
			} catch(SkaianetException e)
			{
				return null;	//TODO handle exception further down the line
			}
		}
		return title;
	}
	
	private static void generateAndSetTitle(World world, PlayerIdentifier player)
	{
		PlayerData data = PlayerSavedData.getData(player, world);
		if(data.getTitle() == null)
		{
			Title title = produceTitle(world, player);
			if(title == null)
				return;
			PlayerSavedData.getData(player, world).setTitle(title);
		} else if(!MinestuckConfig.SERVER.playerSelectedTitle.get())
			LOGGER.warn("Trying to generate a title for {} when a title is already assigned!", player.getUsername());
	}
	
	public static void handlePredefineData(ServerPlayerEntity player, SkaianetException.SkaianetConsumer<PredefineData> consumer) throws SkaianetException
	{
		PlayerIdentifier identifier = IdentifierHandler.encode(player);
		SessionHandler.get(player.server).findOrCreateAndCall(identifier, session -> session.predefineCall(identifier, consumer));
	}
	
	/**
	 * @param c The connection.
	 * @return Damage value for the entry item
	 */
	public static ItemStack getEntryItem(World world, SburbConnection c)
	{
		int color =  ColorHandler.getColorForPlayer(c.getClientIdentifier(), world);
		
		Item artifact;
		switch(c.artifactType)
		{
		case 1: artifact = MSItems.CRUXITE_POTION; break;
		default: artifact = MSItems.CRUXITE_APPLE;
		}
		
		return ColorHandler.setColor(new ItemStack(artifact), color);
	}
	
	public static GristType getPrimaryGristType(PlayerIdentifier player)
	{
		
		return GristTypes.SHALE.get();
	}
	
	public static SburbConnection getConnectionForDimension(ServerWorld world)
	{
		return getConnectionForDimension(world.getServer(), world.getDimension().getType());
	}
	public static SburbConnection getConnectionForDimension(MinecraftServer mcServer, DimensionType dim)
	{
		if(dim == null)
			return null;
		
		return SessionHandler.get(mcServer).getConnectionStream().filter(c -> c.getClientDimension() == dim)
				.findAny().orElse(null);
	}
	
	/**
	 * Calculates the tier of deployables available to the given client player.
	 * Starts at 0 with the first client-server pair, and increases by one for each pair.
	 * If the session chain is sealed and everyone has entered, the tier will be Integer.MAX_VALUE.
	 */
	public static int availableTier(MinecraftServer mcServer, PlayerIdentifier client)
	{
		SessionHandler handler = SessionHandler.get(mcServer);
		Session s = handler.getPlayerSession(client);
		if(s == null)
			return -1;
		if(handler.doesSessionHaveMaxTier(s))
			return Integer.MAX_VALUE;
		SburbConnection c = SkaianetHandler.get(mcServer).getActiveConnection(client);
		if(c == null)
			return -1;
		int count = -1;
		for(SburbConnection conn : s.connections)
			if(conn.hasEntered())
				count++;
		if(!c.hasEntered())
			count++;
		return count;
	}
	
	private static LandTypePair genLandAspects(MinecraftServer mcServer, SburbConnection connection)
	{
		Session session = SessionHandler.get(mcServer).getPlayerSession(connection.getClientIdentifier());
		Title title = PlayerSavedData.getData(connection.getClientIdentifier(), mcServer).getTitle();
		TitleLandType titleLandType = null;
		TerrainLandType terrainLandType = null;
		
		if(session.predefinedPlayers.containsKey(connection.getClientIdentifier()))
		{
			PredefineData data = session.predefinedPlayers.get(connection.getClientIdentifier());
			titleLandType = data.getTitleLandType();
			terrainLandType = data.getTerrainLandType();
		}
		
		if(titleLandType == null)
		{
			if(title.getHeroAspect() == EnumAspect.SPACE && !session.getUsedTitleLandTypes().contains(LandTypes.FROGS) &&
					(terrainLandType == null || LandTypes.FROGS.isAspectCompatible(terrainLandType)))
				titleLandType = LandTypes.FROGS;
			else
			{
				titleLandType = Generator.generateWeightedTitleLandType(session, title.getHeroAspect(), terrainLandType, connection.getClientIdentifier());
				if(terrainLandType != null && titleLandType == LandTypes.TITLE_NULL)
				{
					LOGGER.warn("Failed to find a title land aspect compatible with land aspect \"{}\". Forced to use a poorly compatible land aspect instead.", terrainLandType.getRegistryName());
					titleLandType = Generator.generateWeightedTitleLandType(session, title.getHeroAspect(), null, connection.getClientIdentifier());
				}
			}
		}
		if(terrainLandType == null)
			terrainLandType = Generator.generateWeightedTerrainLandType(session, titleLandType, connection.getClientIdentifier());
		
		return new LandTypePair(terrainLandType, titleLandType);
	}
	
	static void onFirstItemGiven(SburbConnection connection)
	{
		
	}
	
	static void prepareEntry(MinecraftServer mcServer, SburbConnection c)
	{
		PlayerIdentifier identifier = c.getClientIdentifier();
		
		generateAndSetTitle(mcServer.getWorld(DimensionType.OVERWORLD), c.getClientIdentifier());
		LandTypePair landTypes = genLandAspects(mcServer, c);		//This is where the Land dimension is actually registered, but it also needs the player's Title to be determined.
		DimensionType dimType = LandTypes.createLandType(mcServer, identifier, landTypes);
		c.setLand(landTypes, dimType);
	}
	
	static void onEntry(MinecraftServer server, SburbConnection c)
	{
		c.setHasEntered();
		
		SessionHandler.get(server).getPlayerSession(c.getClientIdentifier()).checkIfCompleted();
		
		ServerPlayerEntity player = c.getClientIdentifier().getPlayer(server);
		if(player != null)
		{
			MSCriteriaTriggers.CRUXITE_ARTIFACT.trigger(player);
			c.getLandInfo().sendLandEntryMessage(player);
		}
	}
	
	public static boolean canSelectColor(PlayerIdentifier player, MinecraftServer mcServer)
	{
		return SessionHandler.get(mcServer).getConnectionStream().noneMatch(c -> c.getClientIdentifier().equals(player));
	}
	
	public static boolean hasEntered(ServerPlayerEntity player)
	{
		PlayerIdentifier identifier = IdentifierHandler.encode(player);
		SburbConnection c = SkaianetHandler.get(player.server).getMainConnection(identifier, true);
		return c != null && c.hasEntered();
	}
	
	/**
	 * Extra behavior on the creation of a connection, such as generating the artifact type.
	 */
	static void onConnectionCreated(SburbConnection c)
	{
		Random rand = new Random();	//TODO seed?
		c.artifactType = rand.nextInt(2);
		LOGGER.info("Randomized artifact type to be: {} for player {}.", c.artifactType, c.getClientIdentifier().getUsername());
	}
	
	/**
	 * Checks if the player has the go-ahead to enter.
	 * If the player should get the title selection screen, this will send that packet to the player and then return false.
	 */
	public static boolean performEntryCheck(ServerPlayerEntity player)
	{
		if(!MinestuckConfig.SERVER.playerSelectedTitle.get())
			return true;
		
		PlayerIdentifier identifier = IdentifierHandler.encode(player);
		Session s = SessionHandler.get(player.world).getPlayerSession(identifier);
		
		if(s != null && s.predefinedPlayers.containsKey(identifier) && s.predefinedPlayers.get(identifier).getTitle() != null
				|| PlayerSavedData.getData(identifier, player.server).getTitle() != null)
			return true;
		
		playersInTitleSelection.put(player, new Vec3d(player.getPosX(), player.getPosY(), player.getPosZ()));
		TitleSelectPacket packet = new TitleSelectPacket();
		MSPacketHandler.sendToPlayer(packet, player);
		return false;
	}
	
	public static void cancelSelection(ServerPlayerEntity player)
	{
		playersInTitleSelection.remove(player);
	}
	
	public static void handleTitleSelection(ServerPlayerEntity player, Title title)
	{
		if(MinestuckConfig.SERVER.playerSelectedTitle.get() && playersInTitleSelection.containsKey(player))
		{
			PlayerIdentifier identifier = IdentifierHandler.encode(player);
			
			if(title == null)
				generateAndSetTitle(player.world, identifier);
			else
			{
				Session s = SessionHandler.get(player.server).getPlayerSession(identifier);
				if(s != null && s.getUsedTitles(identifier).contains(title))
				{
					// Title is already used in session; inform the player that they can't pick this title
					MSPacketHandler.sendToPlayer(new TitleSelectPacket(title), player);
					return;
				}
				
				PlayerSavedData.getData(identifier, player.server).setTitle(title);
			}
			
			Vec3d pos = playersInTitleSelection.remove(player);
			
			player.setPosition(pos.x, pos.y, pos.z);
			
			EntryProcess process = new EntryProcess();
			process.onArtifactActivated(player);
			
		} else LOGGER.warn("{} tried to select a title without entering.", player.getName().getFormattedText());
	}
}