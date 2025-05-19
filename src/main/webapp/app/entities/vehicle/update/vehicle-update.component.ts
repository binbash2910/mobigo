import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IPeople } from 'app/entities/people/people.model';
import { PeopleService } from 'app/entities/people/service/people.service';
import { IVehicle } from '../vehicle.model';
import { VehicleService } from '../service/vehicle.service';
import { VehicleFormGroup, VehicleFormService } from './vehicle-form.service';

@Component({
  selector: 'jhi-vehicle-update',
  templateUrl: './vehicle-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class VehicleUpdateComponent implements OnInit {
  isSaving = false;
  vehicle: IVehicle | null = null;

  peopleSharedCollection: IPeople[] = [];

  protected vehicleService = inject(VehicleService);
  protected vehicleFormService = inject(VehicleFormService);
  protected peopleService = inject(PeopleService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: VehicleFormGroup = this.vehicleFormService.createVehicleFormGroup();

  comparePeople = (o1: IPeople | null, o2: IPeople | null): boolean => this.peopleService.comparePeople(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ vehicle }) => {
      this.vehicle = vehicle;
      if (vehicle) {
        this.updateForm(vehicle);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const vehicle = this.vehicleFormService.getVehicle(this.editForm);
    if (vehicle.id !== null) {
      this.subscribeToSaveResponse(this.vehicleService.update(vehicle));
    } else {
      this.subscribeToSaveResponse(this.vehicleService.create(vehicle));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IVehicle>>): void {
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

  protected updateForm(vehicle: IVehicle): void {
    this.vehicle = vehicle;
    this.vehicleFormService.resetForm(this.editForm, vehicle);

    this.peopleSharedCollection = this.peopleService.addPeopleToCollectionIfMissing<IPeople>(
      this.peopleSharedCollection,
      vehicle.proprietaire,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.peopleService
      .query()
      .pipe(map((res: HttpResponse<IPeople[]>) => res.body ?? []))
      .pipe(map((people: IPeople[]) => this.peopleService.addPeopleToCollectionIfMissing<IPeople>(people, this.vehicle?.proprietaire)))
      .subscribe((people: IPeople[]) => (this.peopleSharedCollection = people));
  }
}
