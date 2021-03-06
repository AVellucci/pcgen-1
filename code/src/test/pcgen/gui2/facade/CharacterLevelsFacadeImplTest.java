/*
 * Copyright 2013 (C) James Dempsey <jdempsey@users.sourceforge.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.gui2.facade;


import java.net.URI;

import pcgen.AbstractJunit4CharacterTestCase;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.SkillArmorCheck;
import pcgen.core.Campaign;
import pcgen.core.GameMode;
import pcgen.core.Globals;
import pcgen.core.LevelInfo;
import pcgen.core.PCClass;
import pcgen.core.PlayerCharacter;
import pcgen.core.SettingsHandler;
import pcgen.core.Skill;
import pcgen.core.XPTable;
import pcgen.core.pclevelinfo.PCLevelInfo;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.UIDelegate;
import pcgen.persistence.lst.CampaignSourceEntry;
import pcgen.rules.context.LoadContext;
import pcgen.util.TestHelper;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * CharacterLevelsFacadeImplTest checks that CharacterLevelsFacadeImpl is working ok.
 * 
 * 
 */
public class CharacterLevelsFacadeImplTest extends AbstractJunit4CharacterTestCase
{
	private UIDelegate delegate;
	private TodoManager todoManager;
	private DataSetFacade dataSetFacade;
	
	private static Skill spellcraftSkill;
	private static Skill climbSkill;
	private static Skill umdSkill;
	private static PCClass fighterClass;
	private static PCClass wizardClass;
	

	@Before
	public void before() throws Exception
	{
		delegate = new MockUIDelegate();
		todoManager = new TodoManager();
		final GameMode gameMode = SettingsHandler.getGame();
		dataSetFacade = new MockDataSetFacade(gameMode);
	}

	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		Globals.getContext().resolvePostDeferredTokens();
		Globals.getContext().loadCampaignFacets();
	}

	@Override
	protected void additionalSetUp() throws Exception
	{
		spellcraftSkill = TestHelper.makeSkill("Spellcraft", "Base", intel, false, SkillArmorCheck.NONE);
		climbSkill = TestHelper.makeSkill("Climb", "Base", str, false, SkillArmorCheck.YES);
		umdSkill = TestHelper.makeSkill("Use Magic Device", "Base", str, false, SkillArmorCheck.NONE);
		umdSkill.put(ObjectKey.EXCLUSIVE, true);
		LoadContext context = Globals.getContext();
		
		CampaignSourceEntry source = new CampaignSourceEntry(new Campaign(),
				new URI("file:/" + getClass().getName() + ".java"));
			fighterClass = TestHelper.makeClass("Fighter");
		
		String classPCCText = "CLASS:Fighter	HD:10		TYPE:Base.PC	ABB:Ftr\n"
				+ "CLASS:Fighter	STARTSKILLPTS:2	CSKILL:KEY_Climb|KEY_Use Magic Device";
		fighterClass = TestHelper.parsePCClassText(classPCCText, source);
		context.getReferenceContext().importObject(fighterClass);

		classPCCText = "CLASS:Wizard	HD:4		TYPE:Base.PC	ABB:Wiz\n"
				+ "CLASS:Wizard	STARTSKILLPTS:4	CSKILL:KEY_Spellcraft";
		wizardClass = TestHelper.parsePCClassText(classPCCText, source);
		context.getReferenceContext().importObject(wizardClass);
		context.commit();
		
		context.getReferenceContext().buildDerivedObjects();
		context.resolveDeferredTokens();
	}

	/**
	 * Test method for {@link pcgen.gui2.facade.CharacterLevelsFacadeImpl#findNextLevelForSkill(pcgen.core.facade.SkillFacade, pcgen.core.facade.CharacterLevelFacade, float)}
	 * to check level selection where class and cross-class skills cost the 
	 * same for all classes.
	 */
	@Test
	public void testFindNextLevelForSkillAllSameCost()
	{
		// Set game mode skill cost to be the same for class and cross-slass
		setGameSkillRankData(false);
		
		PlayerCharacter pc = getCharacter();
		pc.incrementClassLevel(4, fighterClass);
		pc.incrementClassLevel(4, wizardClass);
		pc.incrementClassLevel(3, fighterClass);
		pc.incrementClassLevel(3, wizardClass);
		pc.incrementClassLevel(3, fighterClass);
		pc.incrementClassLevel(3, wizardClass);
		
		assertTrue("Climb should be class for fighter", pc.isClassSkill(fighterClass, climbSkill));
		assertFalse("Spellcraft should not be class for fighter", pc.isClassSkill(fighterClass, spellcraftSkill));
		assertTrue("UMD should be class for fighter", pc.isClassSkill(fighterClass, umdSkill));
		assertFalse("Climb should not be class for wizard", pc.isClassSkill(wizardClass, climbSkill));
		assertTrue("Spellcraft should be class for wizard", pc.isClassSkill(wizardClass, spellcraftSkill));
		assertFalse("UMD should be class for fighter", pc.isClassSkill(wizardClass, umdSkill));

		CharacterLevelsFacadeImpl charLvlsFI =
				new CharacterLevelsFacadeImpl(pc, delegate,
					todoManager, dataSetFacade, null);
		
		// Rules for finding the next level to spend a point:

		// 1. Selected level, if points available and not exceeding max ranks.
		assertEquals(
			"Level for spellcraft",
			charLvlsFI.getElementAt(2),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(2), 1.0f));
		assertEquals(
			"Level for spellcraft",
			charLvlsFI.getElementAt(2),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(2), 2.0f));
		assertEquals(
			"Level for spellcraft",
			charLvlsFI.getElementAt(2),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(2), 3.0f));
		assertEquals(
			"Level for climb",
			charLvlsFI.getElementAt(2),
			charLvlsFI.findNextLevelForSkill(climbSkill,
				charLvlsFI.getElementAt(2), 3.0f));
		
		// 2. Scan forward from selected level for first level with points spare 
		// where the rank is not above max rank for the level and the cost is 
		// equal to class cost
		assertEquals(
			"Level for spellcraft - scan forward",
			charLvlsFI.getElementAt(3),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(2), 4.0f));
		assertEquals(
			"Level for rank 20 spellcraft - scan forward",
			charLvlsFI.getElementAt(19),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(2), 20.0f));
		assertEquals(
			"Level for rank 20 climb - scan forward",
			charLvlsFI.getElementAt(19),
			charLvlsFI.findNextLevelForSkill(climbSkill,
				charLvlsFI.getElementAt(2), 20.0f));
		charLvlsFI.getLevelInfo(charLvlsFI.getElementAt(3)).setSkillPointsRemaining(0);
		charLvlsFI.getLevelInfo(charLvlsFI.getElementAt(4)).setSkillPointsRemaining(0);
		assertEquals(
			"Level for rank 4 spellcraft - scan forward past spent levels",
			charLvlsFI.getElementAt(5),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(2), 4.0f));

		// 3. Scan from level 1 for first level with points spare where the rank 
		// is not above max rank for the level and the cost is equal to class cost
		for (int i = 5; i < 20; i++)
		{
			charLvlsFI.getLevelInfo(charLvlsFI.getElementAt(i)).setSkillPointsRemaining(0);
		}
		assertEquals(
			"Level for rank 2 spellcraft - scan from lvl 1 after selected or higher all spent",
			charLvlsFI.getElementAt(1),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(3), 2.0f));
		// 4. Scan forward from selected level for first level with points spare 
		// where the rank is not above max rank for the level and the skill is 
		// not prohibited
		for (int i = 5; i < 10; i++)
		{
			charLvlsFI.getLevelInfo(charLvlsFI.getElementAt(i)).setSkillPointsRemaining(4);
		}
		assertEquals(
			"Level for 1 rank umd - scan forward from lvl 5 for non prohibited class",
			charLvlsFI.getElementAt(8),
			charLvlsFI.findNextLevelForSkill(umdSkill,
				charLvlsFI.getElementAt(5), 1.0f));
		// 5. Scan from level 1 for first level with points spare where the rank 
		// is not above max rank for the level and the skill is not prohibited
		assertEquals(
			"Level for 1 rank umd - scan from lvl 1 as higher all spent for non prohibited class",
			charLvlsFI.getElementAt(0),
			charLvlsFI.findNextLevelForSkill(CharacterLevelsFacadeImplTest.umdSkill, charLvlsFI.getElementAt(17), 1.0f));
		// 6. Advise that the skill cannot be advanced.
		assertNull("Level for rank 21 spellcraft - cannot be advanced",
				charLvlsFI.findNextLevelForSkill(CharacterLevelsFacadeImplTest.spellcraftSkill,
						charLvlsFI.getElementAt(2), 21.0f));
	}

	/**
	 * Test method for {@link pcgen.gui2.facade.CharacterLevelsFacadeImpl#findNextLevelForSkill(pcgen.core.facade.SkillFacade, pcgen.core.facade.CharacterLevelFacade, float)}
	 * to check level selection where cross-class skills cost 2 points and class 
	 * skills cost only 1. 
	 */
	@Test
	public void testFindNextLevelForSkillCrossClass2Points()
	{
		// Set game mode skill cost to be double for cross-class
		setGameSkillRankData(true);
		
		PlayerCharacter pc = getCharacter();
		pc.incrementClassLevel(4, fighterClass);
		pc.incrementClassLevel(4, wizardClass);
		pc.incrementClassLevel(3, fighterClass);
		pc.incrementClassLevel(3, wizardClass);
		pc.incrementClassLevel(3, fighterClass);
		pc.incrementClassLevel(3, wizardClass);
		
		assertTrue("Climb should be class for fighter", pc.isClassSkill(fighterClass, climbSkill));
		assertFalse("Spellcraft should not be class for fighter", pc.isClassSkill(fighterClass, spellcraftSkill));
		assertTrue("UMD should be class for fighter", pc.isClassSkill(fighterClass, umdSkill));
		assertFalse("Climb should not be class for wizard", pc.isClassSkill(wizardClass, climbSkill));
		assertTrue("Spellcraft should be class for wizard", pc.isClassSkill(wizardClass, spellcraftSkill));
		assertFalse("UMD should be class for wizard", pc.isClassSkill(wizardClass, umdSkill));

		CharacterLevelsFacadeImpl charLvlsFI =
				new CharacterLevelsFacadeImpl(pc, delegate,
					todoManager, dataSetFacade, null);
		
		// Rules for finding the next level to spend a point:

		// 1. Selected level, if points available and not exceeding max ranks.
		assertEquals(
			"Level for 1 rank spellcraft",
			charLvlsFI.getElementAt(2),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(2), 1.0f));
		assertEquals(
			"Level for 7 ranks climb",
			charLvlsFI.getElementAt(3),
			charLvlsFI.findNextLevelForSkill(climbSkill,
				charLvlsFI.getElementAt(3), 7.0f));
		assertEquals(
			"Level for 7.5 ranks climb",
			charLvlsFI.getElementAt(4),
			charLvlsFI.findNextLevelForSkill(climbSkill,
				charLvlsFI.getElementAt(4), 7.5f));
		
		// 2. Scan forward from selected level for first level with points spare 
		// where the rank is not above max rank for the level and the cost is 
		// equal to class cost
		assertEquals(
			"Level for 3 ranks spellcraft",
			charLvlsFI.getElementAt(4),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(1), 3.0f));
		assertEquals(
			"Level for 9 ranks spellcraft",
			charLvlsFI.getElementAt(5),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(1), 9.0f));

		// 3. Scan from level 1 for first level with points spare where the rank 
		// is not above max rank for the level and the cost is equal to class cost
		//Note: with the "once class always class" rule for max ranks, this 
		// situation can no longer occur.
//		assertEquals(
//			"Level for 12 ranks climb",
//			charLvlsFI.getElementAt(8),
//			charLvlsFI.findNextLevelForSkill(climbSkill,
//				charLvlsFI.getElementAt(18), 12.0f));

		// 4. Scan forward from selected level for first level with points spare 
		// where the rank is not above max rank for the level and the skill is 
		// not prohibited
		for (int i = 0; i < 20; i++)
		{
			PCLevelInfo pcLevelInfo = charLvlsFI.getLevelInfo(charLvlsFI.getElementAt(i));
			if (pcLevelInfo.getClassKeyName().equals(wizardClass.getKeyName()))
			{
				pcLevelInfo.setSkillPointsRemaining(0);
			}
		}
		assertEquals(
			"Level for 5 ranks spellcraft",
			charLvlsFI.getElementAt(8),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(5), 5.0f));


		// 5. Scan from level 1 for first level with points spare where the rank 
		// is not above max rank for the level and the skill is not prohibited
		assertEquals(
			"Level for 3 ranks spellcraft",
			charLvlsFI.getElementAt(2),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(19), 3.0f));

		// 6. Advise that the skill cannot be advanced.
		charLvlsFI.getLevelInfo(charLvlsFI.getElementAt(18)).setSkillPointsRemaining(4);
		charLvlsFI.getLevelInfo(charLvlsFI.getElementAt(19)).setSkillPointsRemaining(4);
		assertNull("Level for rank 23.5 climb - cannot be advanced", charLvlsFI.findNextLevelForSkill(climbSkill,
				charLvlsFI.getElementAt(2), 23.5f));
		
	}

	/**
	 * Test method for {@link pcgen.gui2.facade.CharacterLevelsFacadeImpl#findNextLevelForSkill(pcgen.core.facade.SkillFacade, pcgen.core.facade.CharacterLevelFacade, float)}
	 * to check level selection where cross-class skills cost 2 points and class 
	 * skills cost only 1. 
	 */
	@Test
	public void testFindNextLevelForSkill35eMultiClass()
	{
		// Set game mode skill cost to be the double for cross-class
		setGameSkillRankData(true);
		
		PlayerCharacter pc = getCharacter();
		pc.incrementClassLevel(1, wizardClass);
		pc.incrementClassLevel(1, fighterClass);
		
		assertTrue("Climb should be class for fighter", pc.isClassSkill(fighterClass, climbSkill));
		assertFalse("Spellcraft should not be class for fighter", pc.isClassSkill(fighterClass, spellcraftSkill));
		assertFalse("Climb should not be class for wizard", pc.isClassSkill(wizardClass, climbSkill));
		assertTrue("Spellcraft should be class for wizard", pc.isClassSkill(wizardClass, spellcraftSkill));

		CharacterLevelsFacadeImpl charLvlsFI =
				new CharacterLevelsFacadeImpl(pc, delegate,
					todoManager, dataSetFacade, null);

		assertEquals(
			"Level for 1 ranks spellcraft",
			charLvlsFI.getElementAt(0),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(0), 1.0f));
		assertEquals(
			"Level for 4 ranks spellcraft",
			charLvlsFI.getElementAt(0),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(0), 4.0f));
		assertEquals(
			"Level for 5 ranks spellcraft",
			charLvlsFI.getElementAt(1),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(0), 5.0f));
		
	}

	/**
	 * Test method for {@link pcgen.gui2.facade.CharacterLevelsFacadeImpl#findNextLevelForSkill(pcgen.core.facade.SkillFacade, pcgen.core.facade.CharacterLevelFacade, float)}
	 * to check level selection for removing a skill rank where class and cross-class skills cost the 
	 * same for all classes.
	 */
	@Test
	public void testFindNextLevelForSkillAllSameCostRemove()
	{
		// Set game mode skill cost to be the same for class and cross-slass
		setGameSkillRankData(false);
		
		PlayerCharacter pc = getCharacter();
		pc.incrementClassLevel(4, fighterClass);
		pc.incrementClassLevel(4, wizardClass);
		pc.incrementClassLevel(3, fighterClass);
		pc.incrementClassLevel(3, wizardClass);
		pc.incrementClassLevel(3, fighterClass);
		pc.incrementClassLevel(3, wizardClass);
		
		assertTrue("Climb should be class for fighter", pc.isClassSkill(fighterClass, climbSkill));
		assertFalse("Spellcraft should not be class for fighter", pc.isClassSkill(fighterClass, spellcraftSkill));
		assertTrue("UMD should be class for fighter", pc.isClassSkill(fighterClass, umdSkill));
		assertFalse("Climb should not be class for wizard", pc.isClassSkill(wizardClass, climbSkill));
		assertTrue("Spellcraft should be class for wizard", pc.isClassSkill(wizardClass, spellcraftSkill));
		assertFalse("UMD should be class for fighter", pc.isClassSkill(wizardClass, umdSkill));

		CharacterLevelsFacadeImpl charLvlsFI =
				new CharacterLevelsFacadeImpl(pc, delegate,
					todoManager, dataSetFacade, null);
		
		charLvlsFI.investSkillPoints(charLvlsFI.getElementAt(0), climbSkill, 1);
		charLvlsFI.investSkillPoints(charLvlsFI.getElementAt(4), spellcraftSkill, 1);
		charLvlsFI.investSkillPoints(charLvlsFI.getElementAt(0), umdSkill, 1);
		charLvlsFI.investSkillPoints(charLvlsFI.getElementAt(1), umdSkill, 1);
		charLvlsFI.investSkillPoints(charLvlsFI.getElementAt(2), umdSkill, 1);
		charLvlsFI.investSkillPoints(charLvlsFI.getElementAt(3), umdSkill, 1);
		charLvlsFI.investSkillPoints(charLvlsFI.getElementAt(8), umdSkill, 2);
		charLvlsFI.investSkillPoints(charLvlsFI.getElementAt(9), umdSkill, 2);
		charLvlsFI.investSkillPoints(charLvlsFI.getElementAt(10), umdSkill, 2);

		// Rules for finding the level to remove a skill rank from:

		// 1. Selected level (if class had purchased a rank, level has spent 
		// points and current rank is not above maxranks)
		assertEquals(
			"Selected level for removing rank of spellcraft",
			charLvlsFI.getElementAt(4),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(4), 0.0f));
		assertEquals(
			"Selected level for removing rank of climb",
			charLvlsFI.getElementAt(0),
			charLvlsFI.findNextLevelForSkill(climbSkill,
				charLvlsFI.getElementAt(0), 0.0f));
		
		// 2. Scan from level 1 for first level of the same class as currently 
		// selected level in which the rank to be removed is below max ranks and 
		// is a class that has bought ranks in the class
		assertEquals(
			"Wizard level for removing rank of spellcraft",
			charLvlsFI.getElementAt(4),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(5), 0.0f));
		assertEquals(
			"Fighter level for removing rank of climb",
			charLvlsFI.getElementAt(0),
			charLvlsFI.findNextLevelForSkill(climbSkill,
				charLvlsFI.getElementAt(16), 0.0f));
		assertEquals(
			"Fighter level for removing ranks 9, 10 of UMD",
			charLvlsFI.getElementAt(9),
			charLvlsFI.findNextLevelForSkill(umdSkill,
				charLvlsFI.getElementAt(16), 8.0f));
		
		// 3. Scan from level 1 for first level of any class in which the rank 
		// to be removed is below max ranks and is a class that has bought 
		// ranks in the class
		assertEquals(
			"Any level for removing rank of spellcraft",
			charLvlsFI.getElementAt(4),
			charLvlsFI.findNextLevelForSkill(spellcraftSkill,
				charLvlsFI.getElementAt(16), 0.0f));
	}

	private void setGameSkillRankData(boolean crossClassCostTwo)
	{
		GameMode game = SettingsHandler.getGame();
		final XPTable xpTable = game.getLevelInfo(game.getDefaultXPTableName());
		LevelInfo levelInfo = xpTable.getLevelInfo(1);
		levelInfo.setLevelString("LEVEL");
		if (crossClassCostTwo)
		{
			levelInfo.setMaxClassSkillString("LEVEL+3");
			levelInfo.setMaxCrossClassSkillString("(LEVEL+3)/2");
			game.setSkillCost_CrossClass(2);
		}
		else
		{
			levelInfo.setMaxClassSkillString("LEVEL");
			levelInfo.setMaxCrossClassSkillString("LEVEL");
			game.setSkillCost_CrossClass(1);
		}
	}
	
}
