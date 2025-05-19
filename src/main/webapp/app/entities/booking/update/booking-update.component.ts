import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IRide } from 'app/entities/ride/ride.model';
import { RideService } from 'app/entities/ride/service/ride.service';
import { IPeople } from 'app/entities/people/people.model';
import { PeopleService } from 'app/entities/people/service/people.service';
import { BookingStatusEnum } from 'app/entities/enumerations/booking-status-enum.model';
import { BookingService } from '../service/booking.service';
import { IBooking } from '../booking.model';
import { BookingFormGroup, BookingFormService } from './booking-form.service';

@Component({
  selector: 'jhi-booking-update',
  templateUrl: './booking-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class BookingUpdateComponent implements OnInit {
  isSaving = false;
  booking: IBooking | null = null;
  bookingStatusEnumValues = Object.keys(BookingStatusEnum);

  ridesSharedCollection: IRide[] = [];
  peopleSharedCollection: IPeople[] = [];

  protected bookingService = inject(BookingService);
  protected bookingFormService = inject(BookingFormService);
  protected rideService = inject(RideService);
  protected peopleService = inject(PeopleService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: BookingFormGroup = this.bookingFormService.createBookingFormGroup();

  compareRide = (o1: IRide | null, o2: IRide | null): boolean => this.rideService.compareRide(o1, o2);

  comparePeople = (o1: IPeople | null, o2: IPeople | null): boolean => this.peopleService.comparePeople(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ booking }) => {
      this.booking = booking;
      if (booking) {
        this.updateForm(booking);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const booking = this.bookingFormService.getBooking(this.editForm);
    if (booking.id !== null) {
      this.subscribeToSaveResponse(this.bookingService.update(booking));
    } else {
      this.subscribeToSaveResponse(this.bookingService.create(booking));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IBooking>>): void {
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

  protected updateForm(booking: IBooking): void {
    this.booking = booking;
    this.bookingFormService.resetForm(this.editForm, booking);

    this.ridesSharedCollection = this.rideService.addRideToCollectionIfMissing<IRide>(this.ridesSharedCollection, booking.trajet);
    this.peopleSharedCollection = this.peopleService.addPeopleToCollectionIfMissing<IPeople>(this.peopleSharedCollection, booking.passager);
  }

  protected loadRelationshipsOptions(): void {
    this.rideService
      .query()
      .pipe(map((res: HttpResponse<IRide[]>) => res.body ?? []))
      .pipe(map((rides: IRide[]) => this.rideService.addRideToCollectionIfMissing<IRide>(rides, this.booking?.trajet)))
      .subscribe((rides: IRide[]) => (this.ridesSharedCollection = rides));

    this.peopleService
      .query()
      .pipe(map((res: HttpResponse<IPeople[]>) => res.body ?? []))
      .pipe(map((people: IPeople[]) => this.peopleService.addPeopleToCollectionIfMissing<IPeople>(people, this.booking?.passager)))
      .subscribe((people: IPeople[]) => (this.peopleSharedCollection = people));
  }
}
