import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { DATE_FORMAT } from 'app/config/input.constants';
import { IPeople } from '../people.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../people.test-samples';

import { PeopleService, RestPeople } from './people.service';

const requireRestSample: RestPeople = {
  ...sampleWithRequiredData,
  dateNaissance: sampleWithRequiredData.dateNaissance?.format(DATE_FORMAT),
};

describe('People Service', () => {
  let service: PeopleService;
  let httpMock: HttpTestingController;
  let expectedResult: IPeople | IPeople[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(PeopleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should create a People', () => {
      const people = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(people).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a People', () => {
      const people = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(people).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a People', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of People', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a People', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    it('should handle exceptions for searching a People', () => {
      const queryObject: any = {
        page: 0,
        size: 20,
        query: '',
        sort: [],
      };
      service.search(queryObject).subscribe(() => expectedResult);

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
      expect(expectedResult).toBe(null);
    });

    describe('addPeopleToCollectionIfMissing', () => {
      it('should add a People to an empty array', () => {
        const people: IPeople = sampleWithRequiredData;
        expectedResult = service.addPeopleToCollectionIfMissing([], people);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(people);
      });

      it('should not add a People to an array that contains it', () => {
        const people: IPeople = sampleWithRequiredData;
        const peopleCollection: IPeople[] = [
          {
            ...people,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addPeopleToCollectionIfMissing(peopleCollection, people);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a People to an array that doesn't contain it", () => {
        const people: IPeople = sampleWithRequiredData;
        const peopleCollection: IPeople[] = [sampleWithPartialData];
        expectedResult = service.addPeopleToCollectionIfMissing(peopleCollection, people);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(people);
      });

      it('should add only unique People to an array', () => {
        const peopleArray: IPeople[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const peopleCollection: IPeople[] = [sampleWithRequiredData];
        expectedResult = service.addPeopleToCollectionIfMissing(peopleCollection, ...peopleArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const people: IPeople = sampleWithRequiredData;
        const people2: IPeople = sampleWithPartialData;
        expectedResult = service.addPeopleToCollectionIfMissing([], people, people2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(people);
        expect(expectedResult).toContain(people2);
      });

      it('should accept null and undefined values', () => {
        const people: IPeople = sampleWithRequiredData;
        expectedResult = service.addPeopleToCollectionIfMissing([], null, people, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(people);
      });

      it('should return initial array if no People is added', () => {
        const peopleCollection: IPeople[] = [sampleWithRequiredData];
        expectedResult = service.addPeopleToCollectionIfMissing(peopleCollection, undefined, null);
        expect(expectedResult).toEqual(peopleCollection);
      });
    });

    describe('comparePeople', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.comparePeople(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 9353 };
        const entity2 = null;

        const compareResult1 = service.comparePeople(entity1, entity2);
        const compareResult2 = service.comparePeople(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 9353 };
        const entity2 = { id: 20275 };

        const compareResult1 = service.comparePeople(entity1, entity2);
        const compareResult2 = service.comparePeople(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 9353 };
        const entity2 = { id: 9353 };

        const compareResult1 = service.comparePeople(entity1, entity2);
        const compareResult2 = service.comparePeople(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
