/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11634")
public class InsertOrderingDuplicateTest
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Product.class,
				SaleDocument.class,
				SaleDocumentItem.class,
				SaleDocumentSummary.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
	}

	@Test
	public void testBatching() throws SQLException {

		doInHibernate( this::sessionFactory, session -> {
			SaleDocumentSummary saleDocumentsummary = new SaleDocumentSummary();
			session.persist(saleDocumentsummary);

			SaleDocumentItem saleDocumentItem = new SaleDocumentItem();
			saleDocumentsummary.addItem( saleDocumentItem );
			session.persist( saleDocumentItem );

			Product product = new Product();
			session.persist(product);
			saleDocumentItem.setProduct(product);

			SaleDocument saleDocument = new SaleDocument();
			session.persist(saleDocument);
			saleDocument.addItem( saleDocumentItem );

			SaleDocument correction = new SaleDocument();
			session.persist(correction);

			saleDocument.setCorerctionSubject(correction);
		} );
	}


	@Entity(name = "Product")
	public static class Product {
		@Id
		@GeneratedValue
		private Long id;

		@Column(unique = true)
		private String name;

		private String description;

		private Integer quantity;

		private BigDecimal price;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(Integer quantity) {
			this.quantity = quantity;
		}

	}

	@Entity(name = "SaleDocument")
	public static class SaleDocument {
		@Id
		@GeneratedValue
		private Long id;

		private String number;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "saleDocument")
		private Set<SaleDocumentItem> items = new HashSet();

		@JoinColumn(name = "ID_SALE_DOCUMENT_CORRECTION", nullable = true)
		@ManyToOne(fetch = FetchType.LAZY)
		private SaleDocument corerctionSubject;

		private BigDecimal totalPrice;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Set<SaleDocumentItem> getItems() {
			return items;
		}

		public void setItems(Set<SaleDocumentItem> items) {
			this.items = items;
		}

		public BigDecimal getTotalPrice() {
			return totalPrice;
		}

		public void setTotalPrice(BigDecimal totalPrice) {
			this.totalPrice = totalPrice;
		}

		public void addItem(SaleDocumentItem sdi) {
			this.getItems().add(sdi);
			sdi.setSaleDocument( this );
		}

		public SaleDocument getCorerctionSubject() {
			return corerctionSubject;
		}

		public void setCorerctionSubject(SaleDocument corerctionSubject) {
			this.corerctionSubject = corerctionSubject;
		}

	}

	@Entity(name = "SaleDocumentItem")
	public class SaleDocumentItem {
		@Id
		@GeneratedValue
		private Long id;

		private Integer lp;

		@ManyToOne(optional = true)
		private Product product;

		@JoinColumn(name = "ID_SALE_DOCUMENT", nullable = true)
		@ManyToOne(fetch = FetchType.LAZY)
		private SaleDocument saleDocument;

		@JoinColumn(name = "ID_SALE_DOCUMENT_SUMAMRY", nullable = true)
		@ManyToOne(fetch = FetchType.LAZY)
		private SaleDocumentSummary summary;

		private Integer quantity;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getLp() {
			return lp;
		}

		public void setLp(Integer lp) {
			this.lp = lp;
		}

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(Integer quantity) {
			this.quantity = quantity;
		}

		public SaleDocument getSaleDocument() {
			return saleDocument;
		}

		public void setSaleDocument(SaleDocument saleDocument) {
			this.saleDocument = saleDocument;
		}

		public SaleDocumentSummary getSummary() {
			return summary;
		}

		public void setSummary(SaleDocumentSummary summary) {
			this.summary = summary;
		}

	}

	@Entity(name = "SaleDocumentSummary")
	public class SaleDocumentSummary {

		@Id
		@GeneratedValue
		private Long id;
		private String number;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "summary")
		private Set<SaleDocumentItem> items = new HashSet();

		private BigDecimal totalPrice;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Set<SaleDocumentItem> getItems() {
			return items;
		}

		public void setItems(Set<SaleDocumentItem> items) {
			this.items = items;
		}

		public BigDecimal getTotalPrice() {
			return totalPrice;
		}

		public void setTotalPrice(BigDecimal totalPrice) {
			this.totalPrice = totalPrice;
		}

		public void addItem(SaleDocumentItem sdi) {
			this.getItems().add(sdi);
			sdi.setSummary( this );
		}
	}
}
