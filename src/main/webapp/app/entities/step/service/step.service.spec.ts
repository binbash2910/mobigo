import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { IStep } from '../step.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../step.test-samples';

import { StepService } from './step.service';

const requireRestSample: IStep = {
  ...sampleWithRequiredData,
};

describe('Step Service', () => {
  let service: StepService;
  let httpMock: HttpTestingController;
  let expectedResult: IStep | IStep[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(StepService);
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

    it('should create a Step', () => {
      const step = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(step).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Step', () => {
      const step = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(step).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Step', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Step', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Step', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    it('should handle exceptions for searching a Step', () => {
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

    describe('addStepToCollectionIfMissing', () => {
      it('should add a Step to an empty array', () => {
        const step: IStep = sampleWithRequiredData;
        expectedResult = service.addStepToCollectionIfMissing([], step);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(step);
      });

      it('should not add a Step to an array that contains it', () => {
        const step: IStep = sampleWithRequiredData;
        const stepCollection: IStep[] = [
          {
            ...step,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addStepToCollectionIfMissing(stepCollection, step);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Step to an array that doesn't contain it", () => {
        const step: IStep = sampleWithRequiredData;
        const stepCollection: IStep[] = [sampleWithPartialData];
        expectedResult = service.addStepToCollectionIfMissing(stepCollection, step);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(step);
      });

      it('should add only unique Step to an array', () => {
        const stepArray: IStep[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const stepCollection: IStep[] = [sampleWithRequiredData];
        expectedResult = service.addStepToCollectionIfMissing(stepCollection, ...stepArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const step: IStep = sampleWithRequiredData;
        const step2: IStep = sampleWithPartialData;
        expectedResult = service.addStepToCollectionIfMissing([], step, step2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(step);
        expect(expectedResult).toContain(step2);
      });

      it('should accept null and undefined values', () => {
        const step: IStep = sampleWithRequiredData;
        expectedResult = service.addStepToCollectionIfMissing([], null, step, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(step);
      });

      it('should return initial array if no Step is added', () => {
        const stepCollection: IStep[] = [sampleWithRequiredData];
        expectedResult = service.addStepToCollectionIfMissing(stepCollection, undefined, null);
        expect(expectedResult).toEqual(stepCollection);
      });
    });

    describe('compareStep', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareStep(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 20214 };
        const entity2 = null;

        const compareResult1 = service.compareStep(entity1, entity2);
        const compareResult2 = service.compareStep(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 20214 };
        const entity2 = { id: 154 };

        const compareResult1 = service.compareStep(entity1, entity2);
        const compareResult2 = service.compareStep(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 20214 };
        const entity2 = { id: 20214 };

        const compareResult1 = service.compareStep(entity1, entity2);
        const compareResult2 = service.compareStep(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
