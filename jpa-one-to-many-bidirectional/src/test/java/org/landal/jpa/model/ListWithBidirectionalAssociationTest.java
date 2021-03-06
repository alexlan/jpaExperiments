package org.landal.jpa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ListWithBidirectionalAssociationTest {

	private static final Logger log = Logger.getLogger(ListWithBidirectionalAssociationTest.class.getName());

	@Deployment
	public static Archive<?> createDeployment() {
		return ShrinkWrap.create(JavaArchive.class, "test.jar").addPackage(BaseBusinessEntity.class.getPackage())
				.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
				.addAsManifestResource("test-persistence.xml", "persistence.xml");
	}

	@PersistenceContext(unitName = "test")
	private EntityManager em;

	@Inject
	private UserTransaction utx;

	@Before
	public void setUp() throws Exception {

		utx.begin();
		em.joinTransaction();

		printStatus("Clearing the database...");
		em.createNamedQuery(SerialKit.DELETE_ALL).executeUpdate();
		em.createNamedQuery(SerialKitStatus.DELETE_ALL).executeUpdate();

		printStatus("Inserting records...");

		SerialKit sk = new SerialKit();
		sk.setCode("SK_CODE");
		sk.setBarcode("SK_BARCODE");
		sk.setStatus(Status.NEW);

		SerialKitStatus sks = new SerialKitStatus();
		sks.setStatus(Status.NEW);
		sks.setOperationDate(new Date());
		sk.addStatus(sks);

		em.persist(sk);
		utx.commit();

	}
	
	@Test
	public void test_one_to_many_bidirectional() throws Exception {
		
		utx.begin();
		em.joinTransaction();
		
		List<SerialKit> skList = em.createNamedQuery(SerialKit.FIND_ALL, SerialKit.class).getResultList();
		assertNotNull(skList);
		assertFalse(skList.isEmpty());
		
		SerialKit serialKit = skList.get(0);
		assertNotNull(serialKit);
		assertNotNull(serialKit.getStatusHistory());
		assertEquals(1,serialKit.getStatusHistory().size());
		
		SerialKitStatus serialKitStatus = serialKit.getStatusHistory().get(0);
		assertNotNull(serialKitStatus);
		
	}

	@Test
	public void test_one_to_many_bidirectional_delete() throws Exception {

		utx.begin();
		em.joinTransaction();

		// delete all previously inserted history items
		List<SerialKit> skList = em.createNamedQuery(SerialKit.FIND_ALL, SerialKit.class).getResultList();
		assertNotNull(skList);
		assertFalse(skList.isEmpty());

		SerialKit serialKit = skList.get(0);
		serialKit.deleteHistory();

		utx.commit();
		utx.begin();
		em.joinTransaction();

		skList = em.createNamedQuery(SerialKit.FIND_ALL, SerialKit.class).getResultList();
		assertTrue(skList.get(0).isHistoryEmpty());

		utx.commit();

	}

	@Test
	public void test_one_to_many_bidirectional_with_fetch() throws Exception {

		utx.begin();
		em.joinTransaction();

		List<SerialKit> skList = em.createNamedQuery(SerialKit.FIND_ALL_WITH_HISTORY, SerialKit.class).getResultList();

		utx.commit();

		assertNotNull(skList);
		assertFalse(skList.isEmpty());
		assertFalse(skList.get(0).isHistoryEmpty());

	}

	private void printStatus(Object message) {
		if (log instanceof Logger) {
			((Logger) log).info(message.toString());
		}
	}

}
