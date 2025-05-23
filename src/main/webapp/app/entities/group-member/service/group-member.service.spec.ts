import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { IGroupMember } from '../group-member.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../group-member.test-samples';

import { GroupMemberService } from './group-member.service';

const requireRestSample: IGroupMember = {
  ...sampleWithRequiredData,
};

describe('GroupMember Service', () => {
  let service: GroupMemberService;
  let httpMock: HttpTestingController;
  let expectedResult: IGroupMember | IGroupMember[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(GroupMemberService);
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

    it('should create a GroupMember', () => {
      const groupMember = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(groupMember).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a GroupMember', () => {
      const groupMember = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(groupMember).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a GroupMember', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of GroupMember', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a GroupMember', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    it('should handle exceptions for searching a GroupMember', () => {
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

    describe('addGroupMemberToCollectionIfMissing', () => {
      it('should add a GroupMember to an empty array', () => {
        const groupMember: IGroupMember = sampleWithRequiredData;
        expectedResult = service.addGroupMemberToCollectionIfMissing([], groupMember);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(groupMember);
      });

      it('should not add a GroupMember to an array that contains it', () => {
        const groupMember: IGroupMember = sampleWithRequiredData;
        const groupMemberCollection: IGroupMember[] = [
          {
            ...groupMember,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addGroupMemberToCollectionIfMissing(groupMemberCollection, groupMember);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a GroupMember to an array that doesn't contain it", () => {
        const groupMember: IGroupMember = sampleWithRequiredData;
        const groupMemberCollection: IGroupMember[] = [sampleWithPartialData];
        expectedResult = service.addGroupMemberToCollectionIfMissing(groupMemberCollection, groupMember);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(groupMember);
      });

      it('should add only unique GroupMember to an array', () => {
        const groupMemberArray: IGroupMember[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const groupMemberCollection: IGroupMember[] = [sampleWithRequiredData];
        expectedResult = service.addGroupMemberToCollectionIfMissing(groupMemberCollection, ...groupMemberArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const groupMember: IGroupMember = sampleWithRequiredData;
        const groupMember2: IGroupMember = sampleWithPartialData;
        expectedResult = service.addGroupMemberToCollectionIfMissing([], groupMember, groupMember2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(groupMember);
        expect(expectedResult).toContain(groupMember2);
      });

      it('should accept null and undefined values', () => {
        const groupMember: IGroupMember = sampleWithRequiredData;
        expectedResult = service.addGroupMemberToCollectionIfMissing([], null, groupMember, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(groupMember);
      });

      it('should return initial array if no GroupMember is added', () => {
        const groupMemberCollection: IGroupMember[] = [sampleWithRequiredData];
        expectedResult = service.addGroupMemberToCollectionIfMissing(groupMemberCollection, undefined, null);
        expect(expectedResult).toEqual(groupMemberCollection);
      });
    });

    describe('compareGroupMember', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareGroupMember(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 25116 };
        const entity2 = null;

        const compareResult1 = service.compareGroupMember(entity1, entity2);
        const compareResult2 = service.compareGroupMember(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 25116 };
        const entity2 = { id: 23461 };

        const compareResult1 = service.compareGroupMember(entity1, entity2);
        const compareResult2 = service.compareGroupMember(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 25116 };
        const entity2 = { id: 25116 };

        const compareResult1 = service.compareGroupMember(entity1, entity2);
        const compareResult2 = service.compareGroupMember(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
