import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IVehicle } from 'app/entities/vehicle/vehicle.model';
import { VehicleService } from 'app/entities/vehicle/service/vehicle.service';
import { RideStatusEnum } from 'app/entities/enumerations/ride-status-enum.model';
import { RideService } from '../service/ride.service';
import { IRide } from '../ride.model';
import { RideFormGroup, RideFormService } from './ride-form.service';

@Component({
  selector: 'jhi-ride-update',
  templateUrl: './ride-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class RideUpdateComponent implements OnInit {
  isSaving = false;
  ride: IRide | null = null;
  rideStatusEnumValues = Object.keys(RideStatusEnum);

  vehiclesSharedCollection: IVehicle[] = [];

  protected rideService = inject(RideService);
  protected rideFormService = inject(RideFormService);
  protected vehicleService = inject(VehicleService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: RideFormGroup = this.rideFormService.createRideFormGroup();

  compareVehicle = (o1: IVehicle | null, o2: IVehicle | null): boolean => this.vehicleService.compareVehicle(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ ride }) => {
      this.ride = ride;
      if (ride) {
        this.updateForm(ride);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const ride = this.rideFormService.getRide(this.editForm);
    if (ride.id !== null) {
      this.subscribeToSaveResponse(this.rideService.update(ride));
    } else {
      this.subscribeToSaveResponse(this.rideService.create(ride));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IRide>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(ride: IRide): void {
    this.ride = ride;
    this.rideFormService.resetForm(this.editForm, ride);

    this.vehiclesSharedCollection = this.vehicleService.addVehicleToCollectionIfMissing<IVehicle>(
      this.vehiclesSharedCollection,
      ride.vehicule,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.vehicleService
      .query()
      .pipe(map((res: HttpResponse<IVehicle[]>) => res.body ?? []))
      .pipe(map((vehicles: IVehicle[]) => this.vehicleService.addVehicleToCollectionIfMissing<IVehicle>(vehicles, this.ride?.vehicule)))
      .subscribe((vehicles: IVehicle[]) => (this.vehiclesSharedCollection = vehicles));
  }
}
