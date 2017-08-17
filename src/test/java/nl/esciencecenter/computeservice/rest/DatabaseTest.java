/**
 * Copyright 2013 Netherlands eScience Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.esciencecenter.computeservice.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import nl.esciencecenter.computeservice.cwl.CWLInputFile;
import nl.esciencecenter.computeservice.cwl.CWLInputFileRepository;

//@RunWith(SpringRunner.class)
//@DataJpaTest
//@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
//@SpringBootTest
//@Import(DatabaseTestConfiguration.class)
//public class DatabaseTest {
//	@Autowired
//	CWLInputFileRepository repository;
//	private static Long theId;
//	
//	@Before
//	public void setUp() {
//		theId = repository.save(new CWLInputFile("/test1/","test.jpg")).getId();
//		repository.save(new CWLInputFile("/test2/","test2.txt"));
//	}
//	
//	@Test
//	public void testDatabaseSave() throws Exception {
//		CWLInputFile newfile = repository.save(new CWLInputFile("/blah/","blah.jpg"));
//		Assert.notNull(newfile.getId(), "The ID after saving should not be null");
//	}
//	
//	@Test
//	public void testDatabaseRetrieve() throws Exception {	
//		CWLInputFile file = repository.findOne(theId);
//		
//		Assert.notNull(file, "The ID " + theId + " was not found in the database");
//	}
//}
