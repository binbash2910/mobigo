import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { DATE_FORMAT } from 'app/config/input.constants';
import { IRating } from '../rating.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../rating.test-samples';

import { RatingService, RestRating } from './rating.service';

const requireRestSample: RestRating = {
  ...sampleWithRequiredData,
  ratingDate: sampleWithRequiredData.ratingDate?.format(DATE_FORMAT),
};

describe('Rating Service', () => {
  let service: RatingService;
  let httpMock: HttpTestingController;
  let expectedResult: IRating | IRating[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(RatingService);
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

    it('should create a Rating', () => {
      const rating = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(rating).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Rating', () => {
      const rating = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(rating).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Rating', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Rating', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Rating', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    it('should handle exceptions for searching a Rating', () => {
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

    describe('addRatingToCollectionIfMissing', () => {
      it('should add a Rating to an empty array', () => {
        const rating: IRating = sampleWithRequiredData;
        expectedResult = service.addRatingToCollectionIfMissing([], rating);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(rating);
      });

      it('should not add a Rating to an array that contains it', () => {
        const rating: IRating = sampleWithRequiredData;
        const ratingCollection: IRating[] = [
          {
            ...rating,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addRatingToCollectionIfMissing(ratingCollection, rating);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Rating to an array that doesn't contain it", () => {
        const rating: IRating = sampleWithRequiredData;
        const ratingCollection: IRating[] = [sampleWithPartialData];
        expectedResult = service.addRatingToCollectionIfMissing(ratingCollection, rating);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(rating);
      });

      it('should add only unique Rating to an array', () => {
        const ratingArray: IRating[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const ratingCollection: IRating[] = [sampleWithRequiredData];
        expectedResult = service.addRatingToCollectionIfMissing(ratingCollection, ...ratingArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const rating: IRating = sampleWithRequiredData;
        const rating2: IRating = sampleWithPartialData;
        expectedResult = service.addRatingToCollectionIfMissing([], rating, rating2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(rating);
        expect(expectedResult).toContain(rating2);
      });

      it('should accept null and undefined values', () => {
        const rating: IRating = sampleWithRequiredData;
        expectedResult = service.addRatingToCollectionIfMissing([], null, rating, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(rating);
      });

      it('should return initial array if no Rating is added', () => {
        const ratingCollection: IRating[] = [sampleWithRequiredData];
        expectedResult = service.addRatingToCollectionIfMissing(ratingCollection, undefined, null);
        expect(expectedResult).toEqual(ratingCollection);
      });
    });

    describe('compareRating', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareRating(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 11381 };
        const entity2 = null;

        const compareResult1 = service.compareRating(entity1, entity2);
        const compareResult2 = service.compareRating(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 11381 };
        const entity2 = { id: 11888 };

        const compareResult1 = service.compareRating(entity1, entity2);
        const compareResult2 = service.compareRating(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 11381 };
        const entity2 = { id: 11381 };

        const compareResult1 = service.compareRating(entity1, entity2);
        const compareResult2 = service.compareRating(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
