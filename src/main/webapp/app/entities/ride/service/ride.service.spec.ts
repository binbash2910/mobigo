import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { DATE_FORMAT } from 'app/config/input.constants';
import { IRide } from '../ride.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../ride.test-samples';

import { RestRide, RideService } from './ride.service';

const requireRestSample: RestRide = {
  ...sampleWithRequiredData,
  dateDepart: sampleWithRequiredData.dateDepart?.format(DATE_FORMAT),
  dateArrivee: sampleWithRequiredData.dateArrivee?.format(DATE_FORMAT),
};

describe('Ride Service', () => {
  let service: RideService;
  let httpMock: HttpTestingController;
  let expectedResult: IRide | IRide[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(RideService);
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

    it('should create a Ride', () => {
      const ride = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(ride).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Ride', () => {
      const ride = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(ride).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Ride', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Ride', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Ride', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    it('should handle exceptions for searching a Ride', () => {
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

    describe('addRideToCollectionIfMissing', () => {
      it('should add a Ride to an empty array', () => {
        const ride: IRide = sampleWithRequiredData;
        expectedResult = service.addRideToCollectionIfMissing([], ride);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(ride);
      });

      it('should not add a Ride to an array that contains it', () => {
        const ride: IRide = sampleWithRequiredData;
        const rideCollection: IRide[] = [
          {
            ...ride,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addRideToCollectionIfMissing(rideCollection, ride);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Ride to an array that doesn't contain it", () => {
        const ride: IRide = sampleWithRequiredData;
        const rideCollection: IRide[] = [sampleWithPartialData];
        expectedResult = service.addRideToCollectionIfMissing(rideCollection, ride);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(ride);
      });

      it('should add only unique Ride to an array', () => {
        const rideArray: IRide[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const rideCollection: IRide[] = [sampleWithRequiredData];
        expectedResult = service.addRideToCollectionIfMissing(rideCollection, ...rideArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const ride: IRide = sampleWithRequiredData;
        const ride2: IRide = sampleWithPartialData;
        expectedResult = service.addRideToCollectionIfMissing([], ride, ride2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(ride);
        expect(expectedResult).toContain(ride2);
      });

      it('should accept null and undefined values', () => {
        const ride: IRide = sampleWithRequiredData;
        expectedResult = service.addRideToCollectionIfMissing([], null, ride, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(ride);
      });

      it('should return initial array if no Ride is added', () => {
        const rideCollection: IRide[] = [sampleWithRequiredData];
        expectedResult = service.addRideToCollectionIfMissing(rideCollection, undefined, null);
        expect(expectedResult).toEqual(rideCollection);
      });
    });

    describe('compareRide', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareRide(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 7624 };
        const entity2 = null;

        const compareResult1 = service.compareRide(entity1, entity2);
        const compareResult2 = service.compareRide(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 7624 };
        const entity2 = { id: 30814 };

        const compareResult1 = service.compareRide(entity1, entity2);
        const compareResult2 = service.compareRide(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 7624 };
        const entity2 = { id: 7624 };

        const compareResult1 = service.compareRide(entity1, entity2);
        const compareResult2 = service.compareRide(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
