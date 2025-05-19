import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { IVehicle } from '../vehicle.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../vehicle.test-samples';

import { VehicleService } from './vehicle.service';

const requireRestSample: IVehicle = {
  ...sampleWithRequiredData,
};

describe('Vehicle Service', () => {
  let service: VehicleService;
  let httpMock: HttpTestingController;
  let expectedResult: IVehicle | IVehicle[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(VehicleService);
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

    it('should create a Vehicle', () => {
      const vehicle = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(vehicle).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Vehicle', () => {
      const vehicle = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(vehicle).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Vehicle', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Vehicle', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Vehicle', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    it('should handle exceptions for searching a Vehicle', () => {
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

    describe('addVehicleToCollectionIfMissing', () => {
      it('should add a Vehicle to an empty array', () => {
        const vehicle: IVehicle = sampleWithRequiredData;
        expectedResult = service.addVehicleToCollectionIfMissing([], vehicle);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(vehicle);
      });

      it('should not add a Vehicle to an array that contains it', () => {
        const vehicle: IVehicle = sampleWithRequiredData;
        const vehicleCollection: IVehicle[] = [
          {
            ...vehicle,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addVehicleToCollectionIfMissing(vehicleCollection, vehicle);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Vehicle to an array that doesn't contain it", () => {
        const vehicle: IVehicle = sampleWithRequiredData;
        const vehicleCollection: IVehicle[] = [sampleWithPartialData];
        expectedResult = service.addVehicleToCollectionIfMissing(vehicleCollection, vehicle);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(vehicle);
      });

      it('should add only unique Vehicle to an array', () => {
        const vehicleArray: IVehicle[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const vehicleCollection: IVehicle[] = [sampleWithRequiredData];
        expectedResult = service.addVehicleToCollectionIfMissing(vehicleCollection, ...vehicleArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const vehicle: IVehicle = sampleWithRequiredData;
        const vehicle2: IVehicle = sampleWithPartialData;
        expectedResult = service.addVehicleToCollectionIfMissing([], vehicle, vehicle2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(vehicle);
        expect(expectedResult).toContain(vehicle2);
      });

      it('should accept null and undefined values', () => {
        const vehicle: IVehicle = sampleWithRequiredData;
        expectedResult = service.addVehicleToCollectionIfMissing([], null, vehicle, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(vehicle);
      });

      it('should return initial array if no Vehicle is added', () => {
        const vehicleCollection: IVehicle[] = [sampleWithRequiredData];
        expectedResult = service.addVehicleToCollectionIfMissing(vehicleCollection, undefined, null);
        expect(expectedResult).toEqual(vehicleCollection);
      });
    });

    describe('compareVehicle', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareVehicle(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 18638 };
        const entity2 = null;

        const compareResult1 = service.compareVehicle(entity1, entity2);
        const compareResult2 = service.compareVehicle(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 18638 };
        const entity2 = { id: 22559 };

        const compareResult1 = service.compareVehicle(entity1, entity2);
        const compareResult2 = service.compareVehicle(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 18638 };
        const entity2 = { id: 18638 };

        const compareResult1 = service.compareVehicle(entity1, entity2);
        const compareResult2 = service.compareVehicle(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
