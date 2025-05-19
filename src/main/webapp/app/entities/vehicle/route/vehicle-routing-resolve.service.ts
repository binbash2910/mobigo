import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IVehicle } from '../vehicle.model';
import { VehicleService } from '../service/vehicle.service';

const vehicleResolve = (route: ActivatedRouteSnapshot): Observable<null | IVehicle> => {
  const id = route.params.id;
  if (id) {
    return inject(VehicleService)
      .find(id)
      .pipe(
        mergeMap((vehicle: HttpResponse<IVehicle>) => {
          if (vehicle.body) {
            return of(vehicle.body);
          }
          inject(Router).navigate(['404']);
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default vehicleResolve;
