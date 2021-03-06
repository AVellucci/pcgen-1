/*
 * Copyright (c) 2007 Tom Parker <thpr@users.sourceforge.net>
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package plugin.lsttokens;

import org.junit.Test;

import pcgen.cdom.base.CDOMObject;
import pcgen.core.PCTemplate;
import pcgen.persistence.PersistenceLayerException;
import pcgen.rules.persistence.CDOMLoader;
import pcgen.rules.persistence.token.CDOMPrimaryToken;
import plugin.lsttokens.testsupport.AbstractGlobalTokenTestCase;
import plugin.lsttokens.testsupport.CDOMTokenLoader;
import plugin.lsttokens.testsupport.ConsolidationRule;

public class MoveLstTest extends AbstractGlobalTokenTestCase
{
	static CDOMPrimaryToken<CDOMObject> token = new MoveLst();
	static CDOMTokenLoader<PCTemplate> loader = new CDOMTokenLoader<>();

	@Override
	public CDOMLoader<PCTemplate> getLoader()
	{
		return loader;
	}

	@Override
	public Class<PCTemplate> getCDOMClass()
	{
		return PCTemplate.class;
	}

	@Override
	public CDOMPrimaryToken<CDOMObject> getToken()
	{
		return token;
	}

//	@Test
//	public void testInvalidObject() throws PersistenceLayerException
//	{
//		assertFalse(token.parse(primaryContext, new Equipment(),
//				"Fly,40"));
//	}

	@Test
	public void testInvalidInputEmpty() throws PersistenceLayerException
	{
		assertFalse(parse(""));
		assertNoSideEffects();
	}

	@Test
	public void testInvalidInputOneItem() throws PersistenceLayerException
	{
		assertFalse(parse("Normal"));
		assertNoSideEffects();
	}

	@Test
	public void testInvalidInputNoValue() throws PersistenceLayerException
	{
		assertFalse(parse("Normal,"));
		assertNoSideEffects();
	}

	@Test
	public void testInvalidInputOnlyValue() throws PersistenceLayerException
	{
		assertFalse(parse(",30"));
		assertNoSideEffects();
	}

	@Test
	public void testInvalidInputTwoComma() throws PersistenceLayerException
	{
		assertFalse(parse("Normal,,30"));
		assertNoSideEffects();
	}

	@Test
	public void testInvalidInputThreeItems() throws PersistenceLayerException
	{
		assertFalse(parse("Normal,30,Darkvision"));
		assertNoSideEffects();
	}

	@Test
	public void testInvalidInputNegativeMovement()
		throws PersistenceLayerException
	{
		assertFalse(parse("Normal,-30"));
		assertNoSideEffects();
	}

	@Test
	public void testInvalidInputNaNMovement()
		throws PersistenceLayerException
	{
		assertFalse(parse("Normal,Foo"));
		assertNoSideEffects();
	}

	@Test
	public void testValidInputNumber()
		throws PersistenceLayerException
	{
		assertTrue(parse("30"));
		String[] unparsed = getToken().unparse(primaryContext, primaryProf);
		assertNotNull(unparsed);
		assertEquals(1, unparsed.length);
		assertEquals("Expected item to be equal", "Walk,30", unparsed[0]);
	}

	@Test
	public void testRoundRobinSimple() throws PersistenceLayerException
	{
		runRoundRobin("Walk,30");
	}

	@Test
	public void testRoundRobinZero() throws PersistenceLayerException
	{
		runRoundRobin("Darkvision,0");
	}

	@Test
	public void testRoundRobinMultiple() throws PersistenceLayerException
	{
		runRoundRobin("Darkvision,0,Walk,30");
	}

	@Override
	protected String getLegalValue()
	{
		return "Darkvision,0";
	}

	@Override
	protected String getAlternateLegalValue()
	{
		return "Low-Light,0,Walk,30";
	}

	@Override
	protected ConsolidationRule getConsolidationRule()
	{
		return ConsolidationRule.SEPARATE;
	}
}
